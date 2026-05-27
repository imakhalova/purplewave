# PurpleWave Auction — Field Capture App

Android app for capturing auction equipment data in offline environments. Items are persisted locally with Room and synced in the background using WorkManager when network connectivity becomes available. Sync status updates reactively in the UI.

## Architecture

**MVVM + Repository** — each layer has one job:

| Layer | Responsibility |
|---|---|
| `domain/` | Plain Kotlin models (`AuctionItem`, `SyncStatus`) — no Android deps |
| `data/local/` | Room entity, DAO, type converter, database |
| `data/remote/` | Retrofit interface (`AuctionApi`) + `FakeAuctionApi` implementation |
| `data/AuctionRepository` | Single source of truth; maps entities ↔ domain models, owns upload and reconciliation logic |
| `worker/SyncWorker` | WorkManager `CoroutineWorker`; uploads pending items then fetches and merges server list |
| `ui/*/ViewModel` | Exposes `StateFlow<UiState>` collected by Compose screens |

I used a **manual ServiceLocator** instead of Hilt. Hilt would be the right choice in a production project, but it adds annotation-processor overhead and boilerplate that felt out of scope for 3 hours. The swap is trivial: replace `ServiceLocator.provideRepository()` with `@Inject` constructor + `@HiltViewModel`.

## WorkManager & Connectivity Restoration

Every sync request is built with:

```kotlin
Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
```

WorkManager holds queued work until the constraint is satisfied. When the device regains connectivity, WorkManager releases the held request and the `SyncWorker` runs — no `BroadcastReceiver`, no `ConnectivityManager` polling. Syncs are enqueued as **unique work** (`ExistingWorkPolicy.KEEP`) so capturing multiple items rapidly never stacks duplicate Workers. Two triggers enqueue a sync:

1. **Item captured** — `CaptureViewModel.saveItem()` enqueues immediately after the DB insert.
2. **Manual "Sync Now"** — button in the list toolbar, useful for testing.

Because the constraint re-evaluates on connectivity change, any items that are still `PENDING` or `FAILED` when the device goes back online will be picked up automatically on the next trigger.

## ID Strategy

Three identifiers, each with a distinct role:

- **`id`** — Room auto-increment PK, local to this device only. Never sent to the server or shared with other devices.
- **`clientId`** — UUID generated at capture time on the device. Sent with every upload attempt as an idempotency key so the server can deduplicate retries. Stable for the lifetime of the item. Unique across devices — two field agents capturing different items will never produce the same `clientId`.
- **`serverId`** — Opaque ID assigned by the server on successful upload. Stored locally so reconciliation can match server-list items back to local rows. Null until the server confirms the upload.

Using `clientId` as the match key (rather than the local auto-increment `id`) is what makes multi-device sync correct — two devices independently auto-incrementing from `id = 1` would collide, but UUIDs never will.

## Server Reconciliation (Three-Way Merge)

After uploading pending items, `SyncWorker` calls `GET /items` and merges the result into the local DB. Three cases for every item the server returns:

1. **Matches a local PENDING / UPLOADING / FAILED row by `clientId`** — the client owns this item and is still trying to confirm it. Leave it alone; local state is authoritative until the server formally accepts it.
2. **Matches a local SYNCED row by `clientId`** — both sides agree. Nothing to do.
3. **Unknown `clientId`** — uploaded by a different device. Insert locally as SYNCED so this device's list stays complete.

Additionally, any locally-SYNCED item whose `serverId` is absent from the server response is reset to PENDING — the server may have purged it and it needs to be re-uploaded.

## Fake Network Rules

`FakeAuctionApi` uses an `AtomicInteger` counter. Every 3rd call returns HTTP 500; all others return HTTP 200 after an 800 ms delay. Successful uploads are stored in a `ConcurrentHashMap` keyed by `clientId`, so `fetchItems()` returns a realistic server list and re-uploading the same item (retry after timeout) returns the same `serverId` rather than creating a duplicate.

## What I Would Add With More Time

- **Hilt** for proper dependency injection with test-friendly component swapping.
- **Room in-memory database tests** for DAO queries (`getPendingOrFailed`, `getBySyncStatus`, `getByClientId`).
- **CameraX** for real photo capture, storing the content URI in the entity.
- **Photo upload Worker** — a separate Worker that fires after metadata sync succeeds, uploading photo binaries to a pre-signed URL independently so metadata and photos can retry without blocking each other.
- **PeriodicWorkRequest** (e.g. every 15 min) as a safety net to catch items that slipped through one-time triggers.
- **WorkManager observer** in the ViewModel — subscribe to `WorkInfo` by unique work name so the UI can show a "sync in progress" banner driven by WorkManager state rather than only Room state.
- **Pagination** for the server fetch (`GET /items?since=<timestamp>`) — fetching the full list on every sync is fine at small scale but won't hold up as the auction catalogue grows.

## Conscious Tradeoffs

- **Sequential uploads in the Worker** — simpler failure accounting and deterministic fake counter. Parallel uploads would be faster but require careful concurrent status tracking.
- **`Result.success()` on partial upload failure** — failed items stay in the DB with `FAILED` status and retry on the next sync. Returning `Result.retry()` would re-run the entire batch including items that already succeeded, which is wasteful.
- **`syncFromServer()` failure is non-fatal** — if the fetch fails after uploads succeed, the Worker still returns `Result.success()`. Uploads are not rolled back. The next sync will re-run the fetch.
- **No Hilt** — saved ~30 min of setup at the cost of a less idiomatic DI pattern.
- **Photos excluded from the metadata sync** — field photos can be several MB each; bundling them into the same sync request makes uploads slow and fragile on poor connectivity. The `photoUri` in Room is the hook for a future dedicated photo upload call. For the exercise, `PHOTO_CAPTURED_SENTINEL` signals that a photo was taken and the UI maps it to a bundled drawable standing in for CameraX output.
- **One photo per item** — `AuctionItemEntity` has a single `photo_uri` column. A production schema would give photos their own table with a foreign key to `auction_items` to support multiple shots per item.
- **`sync_status` and `client_id` and `server_id` columns are all indexed** — `getPendingOrFailed()` and `getBySyncStatus()` filter by status on every sync; `getByClientId()` is called once per server item during reconciliation. Without indexes these are full table scans.
- **Full server list fetch** — `GET /items` returns everything the server knows. A `since` timestamp parameter would limit the payload to items changed after the last sync, which is the right approach once the catalogue is large.
