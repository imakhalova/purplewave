# PurpleWave Auction — Field Capture App

Android app for capturing auction equipment data in offline environments. Items are persisted locally with Room and synced in the background using WorkManager when network connectivity becomes available. Sync status updates reactively in the UI.

## Architecture

**MVVM + Repository** — each layer has one job:

| Layer | Responsibility |
|---|---|
| `domain/` | Plain Kotlin models (`AuctionItem`, `SyncStatus`) — no Android deps |
| `data/local/` | Room entity, DAO, type converter, database |
| `data/remote/` | Retrofit interface (`AuctionApi`) + `FakeAuctionApi` implementation |
| `data/AuctionRepository` | Single source of truth; maps entities ↔ domain models, owns upload logic |
| `worker/SyncWorker` | WorkManager `CoroutineWorker`; iterates pending items, delegates to repository |
| `ui/*/ViewModel` | Exposes `StateFlow<UiState>` collected by Compose screens |

I used a **manual ServiceLocator** instead of Hilt. Hilt would be the right choice in a production project, but it adds annotation-processor overhead and boilerplate that felt out of scope for 3 hours. The swap is trivial: replace `ServiceLocator.provideRepository()` with `@Inject` constructor + `@HiltViewModel`.

## WorkManager & Connectivity Restoration

Every sync request is built with:

```kotlin
Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
```

WorkManager holds queued work until the constraint is satisfied. When the device regains connectivity, WorkManager releases the held request and the `SyncWorker` runs — no `BroadcastReceiver`, no `ConnectivityManager` polling. Two triggers enqueue a sync:

1. **Item captured** — `CaptureViewModel.saveItem()` enqueues immediately after the DB insert.  
2. **Manual "Sync Now"** — button in the list toolbar, useful for testing.

Because the constraint re-evaluates on connectivity change, any items that are still `PENDING` or `FAILED` when the device goes back online will be picked up automatically on the next trigger.

## Fake Network Rules

`FakeAuctionApi` uses an `AtomicInteger` counter. Every 3rd call returns HTTP 500; all others return HTTP 200 after an 800 ms delay. The counter is shared across all items in a single Worker run, so the failure pattern is stable and testable (reset on restart).

## What I Would Add With More Time

- **DI framework** (e.g Hilt, Koin) for proper dependency injection with test-friendly component swapping.
- **GET query** to be able to receive latest updates from server and merging logic for local and server changes.
- **Room in-memory database test** for the DAO queries (especially `getPendingOrFailed`).
- **CameraX** for real photo capture and storing the URI in the entity.
- **Image compression and upload queue***
- **PeriodicWorkRequest** (e.g. every 15 min) as a safety net for items that slipped through.
- **WorkManager observer** in the ViewModel — subscribe to `WorkInfo` by tag so the UI can show a "sync in progress" banner driven by WorkManager state rather than only Room state.

## Conscious Tradeoffs

- **Sequential uploads in the Worker** — simpler failure accounting and deterministic fake counter. Parallel uploads would be faster but requires careful concurrent status tracking.
- **`Result.success()` on partial failure** — items stay `FAILED` in the DB and retry next sync. Returning `Result.retry()` for any failure would re-run the entire batch (including items that already succeeded), which is wasteful.
- **No Hilt** — saved ~30 min of setup at the cost of a less idiomatic DI pattern.
- **Photos excluded from the metadata sync** — the `SyncWorker` uploads item metadata (title, description, condition, timestamps) but not the photo binary. High-resolution field photos can be several MB each; bundling them into the same sync request would make uploads slow, fragile on poor connectivity, and expensive in bandwidth. The right pattern is a separate upload call — either a dedicated Worker that fires after the metadata sync succeeds, or a multipart PUT to a pre-signed URL — so metadata reaches the server quickly and photo upload can retry independently without re-sending all fields. The `photoUri` stored in Room is the hook for that future call. For the exercise, a `PHOTO_CAPTURED_SENTINEL` string signals that a photo was taken; the UI maps it to the bundled drawable, standing in for what CameraX would produce.
- **Limited to 1 photo** - `AuctionItemEntity` accommodates only 1 photo, while photos deserve their own entity and DB relationship to auction entity
- **`sync_status` column is indexed** — `getPendingOrFailed()` filters by status on every sync trigger; without the index that's a full table scan. Added `index = true` on the `AuctionItemEntity` column annotation.
