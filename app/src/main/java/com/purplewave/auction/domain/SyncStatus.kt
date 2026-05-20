package com.purplewave.auction.domain

/**
 * Lifecycle of a captured item's server-sync state.
 *
 * pending   → item saved locally, never attempted
 * uploading → Worker has claimed the item and is trying right now
 * synced    → server acknowledged the upload
 * failed    → most-recent attempt failed; eligible for retry on next sync
 */
enum class SyncStatus {
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED
}
