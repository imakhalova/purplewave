package com.purplewave.auction.data.local

import androidx.room.*
import com.purplewave.auction.domain.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AuctionItemDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** All items, newest-captured first. Flow keeps the UI reactive. */
    @Query("SELECT * FROM auction_items ORDER BY captured_at_ms DESC")
    fun observeAll(): Flow<List<AuctionItemEntity>>

    /** Items that need to be (re)uploaded: pending or failed. */
    @Query(
        "SELECT * FROM auction_items WHERE sync_status IN ('PENDING','FAILED') ORDER BY captured_at_ms ASC"
    )
    suspend fun getPendingOrFailed(): List<AuctionItemEntity>

    /** All items with a specific sync status. Used for server reconciliation. */
    @Query("SELECT * FROM auction_items WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<AuctionItemEntity>

    @Query("SELECT * FROM auction_items WHERE id = :id")
    suspend fun getById(id: Long): AuctionItemEntity?

    /** Lookup by clientId — used during server reconciliation to avoid inserting
     *  items that already exist locally under a different Room id. */
    @Query("SELECT * FROM auction_items WHERE client_id = :clientId LIMIT 1")
    suspend fun getByClientId(clientId: String): AuctionItemEntity?

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AuctionItemEntity): Long

    /**
     * Targeted status + serverId update after a successful upload.
     * Storing serverId here is what makes cross-device reconciliation possible.
     */
    @Query(
        "UPDATE auction_items SET sync_status = :status, sync_attempts = :attempts, server_id = :serverId WHERE id = :id"
    )
    suspend fun updateSyncState(id: Long, status: SyncStatus, attempts: Int, serverId: String? = null)

    /**
     * Resets an item to PENDING using its serverId.
     * Used during reconciliation when the server no longer has an item we thought was synced.
     */
    @Query(
        "UPDATE auction_items SET sync_status = 'PENDING', sync_attempts = 0 WHERE server_id = :serverId"
    )
    suspend fun resetToUnsynced(serverId: String)

    @Delete
    suspend fun delete(item: AuctionItemEntity)
}
