package com.purplewave.auction.data

import com.purplewave.auction.data.local.AuctionItemDao
import com.purplewave.auction.data.local.AuctionItemEntity
import com.purplewave.auction.data.remote.AuctionApi
import com.purplewave.auction.data.remote.UploadRequest
import com.purplewave.auction.domain.AuctionItem
import com.purplewave.auction.domain.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for auction items.
 *
 * The repository owns the mapping between the Room entity and the domain model,
 * keeping persistence details out of the ViewModel and Worker.
 */
class AuctionRepository(
    private val dao: AuctionItemDao,
    private val api: AuctionApi
) {

    // ── Observe ───────────────────────────────────────────────────────────────

    val items: Flow<List<AuctionItem>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    suspend fun saveItem(item: AuctionItem): Long =
        dao.insert(item.toEntity())

    suspend fun updateSyncState(id: Long, status: SyncStatus, attempts: Int) =
        dao.updateSyncState(id, status, attempts)

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Returns all items that are ready for an upload attempt.
     * Called by [SyncWorker] inside the background process.
     */
    suspend fun getPendingItems(): List<AuctionItem> =
        dao.getPendingOrFailed().map { it.toDomain() }

    /**
     * Attempts to upload a single item. Updates [SyncStatus] in Room before
     * and after the attempt so the UI stays reactive throughout.
     *
     * Returns true if the upload succeeded.
     */
    suspend fun uploadItem(item: AuctionItem): Boolean {
        val newAttempts = item.syncAttempts + 1

        // Mark as uploading so the UI can show progress immediately
        dao.updateSyncState(item.id, SyncStatus.UPLOADING, newAttempts)

        return try {
            val response = api.uploadItem(
                UploadRequest(
                    id = item.id,
                    title = item.title,
                    description = item.description,
                    conditionRating = item.conditionRating,
                    capturedAtMs = item.capturedAtMs
                )
            )
            if (response.isSuccessful) {
                dao.updateSyncState(item.id, SyncStatus.SYNCED, newAttempts)
                true
            } else {
                dao.updateSyncState(item.id, SyncStatus.FAILED, newAttempts)
                false
            }
        } catch (e: Exception) {
            dao.updateSyncState(item.id, SyncStatus.FAILED, newAttempts)
            false
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun AuctionItemEntity.toDomain() = AuctionItem(
        id = id,
        title = title,
        description = description,
        conditionRating = conditionRating,
        photoUri = photoUri,
        syncStatus = syncStatus,
        capturedAtMs = capturedAtMs,
        syncAttempts = syncAttempts
    )

    private fun AuctionItem.toEntity() = AuctionItemEntity(
        id = id,
        title = title,
        description = description,
        conditionRating = conditionRating,
        photoUri = photoUri,
        syncStatus = syncStatus,
        capturedAtMs = capturedAtMs,
        syncAttempts = syncAttempts
    )
}
