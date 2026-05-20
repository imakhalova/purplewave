package com.purplewave.auction.worker

import android.content.Context
import androidx.work.*
import com.purplewave.auction.data.AuctionRepository
import com.purplewave.auction.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager [CoroutineWorker] that uploads all pending/failed items.
 *
 * Design decisions:
 *  • Uses [NETWORK_CONNECTED] constraint so WorkManager itself handles
 *    "run when connectivity is restored" — no manual BroadcastReceiver needed.
 *  • Items are uploaded sequentially (not in parallel) to keep the fake 1-in-3
 *    failure counter deterministic and to avoid hammering a real endpoint.
 *  • On partial failure the Worker returns [Result.success()] so WorkManager
 *    does NOT retry the whole batch. Failed items retain FAILED status and will
 *    be picked up on the next sync trigger (enqueued on next capture or next
 *    connectivity event).
 *  • If an unexpected exception escapes, [Result.retry()] is returned and
 *    WorkManager will back off exponentially.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository: AuctionRepository = ServiceLocator.provideRepository(applicationContext)

        try {
            val pending = repository.getPendingItems()
            if (pending.isEmpty()) return@withContext Result.success()

            pending.forEach { item ->
                // uploadItem handles its own status transitions in Room
                repository.uploadItem(item)
            }

            Result.success()
        } catch (e: Exception) {
            // Unexpected error (e.g. DB corruption) — let WorkManager retry
            Result.retry()
        }
    }

    companion object {
        const val WORK_TAG = "auction_sync"
        const val WORK_NAME = "auction_sync_unique"

        /**
         * Builds a one-time sync request with a network constraint.
         * WorkManager will queue this immediately if online, or hold it until
         * connectivity is restored — no connectivity BroadcastReceiver needed.
         */
        fun buildRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(WORK_TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
    }
}
