package com.purplewave.auction.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.purplewave.auction.domain.SyncStatus

/**
 * Room entity. Using explicit column names so schema changes don't silently
 * break queries if a field is renamed in the future.
 *
 * ID strategy — three identifiers, each with a distinct role:
 *
 *  [id]        Auto-increment PK, local to this device. Used internally by Room
 *              and WorkManager. Never sent to the server or shared with other devices.
 *
 *  [clientId]  UUID generated at capture time on the device. Sent with every upload
 *              attempt so the server can deduplicate retries (idempotency key).
 *              Stable for the lifetime of the item regardless of sync state.
 *              Unique across devices — two field agents capturing different items
 *              will never produce the same clientId.
 *
 *  [serverId]  Opaque ID assigned by the server on successful upload. Stored locally
 *              so that [AuctionRepository.syncFromServer] can match server-list items
 *              back to local rows by serverId rather than the device-local [id].
 *              Null until the server confirms the upload.
 */
@Entity(
    tableName = "auction_items",
    indices = [
        Index(value = ["sync_status"]),         // fast filtering in getPendingOrFailed
        Index(value = ["client_id"], unique = true), // prevent duplicate captures
        Index(value = ["server_id"])             // fast lookup during reconciliation
    ]
)
data class AuctionItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** Device-generated UUID. Assigned once at capture, never changes. */
    @ColumnInfo(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    /** 1–5 star condition rating */
    @ColumnInfo(name = "condition_rating")
    val conditionRating: Int,

    /** Local file URI for photo, or null when using placeholder */
    @ColumnInfo(name = "photo_uri")
    val photoUri: String?,

    /** Stored as the enum name string via SyncStatusConverter */
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus,

    /** Epoch millis when the field agent captured this item */
    @ColumnInfo(name = "captured_at_ms")
    val capturedAtMs: Long,

    /** Running count of upload attempts; used to surface in UI / future back-off tuning */
    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0,

    /** Server-assigned ID. Null until the server confirms upload. */
    @ColumnInfo(name = "server_id")
    val serverId: String? = null
)
