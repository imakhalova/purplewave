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

    @Query("SELECT * FROM auction_items WHERE id = :id")
    suspend fun getById(id: Long): AuctionItemEntity?

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AuctionItemEntity): Long

    /**
     * Targeted status update — avoids re-writing the whole row and prevents
     * races where a Worker update would clobber a concurrent insert.
     */
    @Query(
        "UPDATE auction_items SET sync_status = :status, sync_attempts = :attempts WHERE id = :id"
    )
    suspend fun updateSyncState(id: Long, status: SyncStatus, attempts: Int)

    @Delete
    suspend fun delete(item: AuctionItemEntity)
}
