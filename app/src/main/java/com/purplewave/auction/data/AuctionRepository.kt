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

    // ── Sync ──────────────────────────────────────────────────────────────────

    /** Returns all items that are ready for an upload attempt. */
    suspend fun getPendingItems(): List<AuctionItem> =
        dao.getPendingOrFailed().map { it.toDomain() }

    /**
     * Attempts to upload a single item. Updates [SyncStatus] in Room before
     * and after the attempt so the UI stays reactive throughout.
     *
     * On success, stores the server-assigned [serverId] in Room. This is the
     * canonical cross-device identifier used for reconciliation in [syncFromServer].
     * The upload payload uses [AuctionItem.clientId] as an idempotency key so
     * the server can deduplicate retries without creating duplicate records.
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
                    clientId = item.clientId,
                    title = item.title,
                    description = item.description,
                    conditionRating = item.conditionRating,
                    capturedAtMs = item.capturedAtMs
                )
            )
            if (response.isSuccessful) {
                val serverId = response.body()?.serverId
                dao.updateSyncState(item.id, SyncStatus.SYNCED, newAttempts, serverId)
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

    /**
     * Fetches the server's current item list and performs a full three-way merge
     * into the local DB.
     *
     * Three cases for every item the server returns:
     *
     *  1. Server item matches a local row by [clientId] and that row is PENDING /
     *     UPLOADING / FAILED — the client owns this item and is still trying to
     *     confirm it. Leave it alone; local state is authoritative until the server
     *     has formally accepted it.
     *
     *  2. Server item matches a local row by [clientId] and that row is already
     *     SYNCED — nothing to do, both sides agree.
     *
     *  3. Server item has a [clientId] we have never seen — it was captured and
     *     uploaded by a different device. Insert it locally as SYNCED so this
     *     device's list stays complete.
     *
     * Additionally, any locally-SYNCED item whose [serverId] is absent from the
     * server response is reset to PENDING — the server may have purged it and it
     * needs to be re-uploaded.
     *
     * Returns true if the fetch succeeded.
     */
    suspend fun syncFromServer(): Boolean {
        return try {
            val response = api.fetchItems()
            if (!response.isSuccessful) return false

            val serverItems = response.body() ?: emptyList()
            val knownServerIds = serverItems.map { it.serverId }.toSet()

            // ── Case 3: insert items from other devices we've never seen ──────
            serverItems.forEach { serverItem ->
                val existing = dao.getByClientId(serverItem.clientId)
                if (existing == null) {
                    // Brand-new item from another device — insert as SYNCED.
                    // We use clientId from the server payload as our local clientId
                    // so future reconciliation rounds still match correctly.
                    dao.insert(
                        AuctionItemEntity(
                            clientId = serverItem.clientId,
                            title = serverItem.title,
                            description = serverItem.description,
                            conditionRating = serverItem.conditionRating,
                            photoUri = null,          // remote items have no local photo yet
                            syncStatus = SyncStatus.SYNCED,
                            capturedAtMs = serverItem.capturedAtMs,
                            syncAttempts = 0,
                            serverId = serverItem.serverId
                        )
                    )
                }
                // Cases 1 & 2 — local row already exists, leave it untouched.
                // If it's PENDING/FAILED the Worker will upload it on next sync.
                // If it's already SYNCED both sides agree, nothing to do.
            }

            // ── Orphan detection: locally-SYNCED but gone from server ─────────
            dao.getBySyncStatus(SyncStatus.SYNCED)
                .filter { it.serverId != null && it.serverId !in knownServerIds }
                .forEach { orphan -> dao.resetToUnsynced(orphan.serverId!!) }

            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun AuctionItemEntity.toDomain() = AuctionItem(
        id = id,
        clientId = clientId,
        title = title,
        description = description,
        conditionRating = conditionRating,
        photoUri = photoUri,
        syncStatus = syncStatus,
        capturedAtMs = capturedAtMs,
        syncAttempts = syncAttempts,
        serverId = serverId
    )

    private fun AuctionItem.toEntity() = AuctionItemEntity(
        id = id,
        clientId = clientId,
        title = title,
        description = description,
        conditionRating = conditionRating,
        photoUri = photoUri,
        syncStatus = syncStatus,
        capturedAtMs = capturedAtMs,
        syncAttempts = syncAttempts,
        serverId = serverId
    )
}
