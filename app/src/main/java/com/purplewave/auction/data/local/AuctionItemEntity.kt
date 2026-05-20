package com.purplewave.auction.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.purplewave.auction.domain.SyncStatus

/**
 * Room entity. Using explicit column names so schema changes don't silently
 * break queries if a field is renamed in the future.
 */
@Entity(tableName = "auction_items")
data class AuctionItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

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
    @ColumnInfo(name = "sync_status", index = true)
    val syncStatus: SyncStatus,

    /** Epoch millis when the field agent captured this item */
    @ColumnInfo(name = "captured_at_ms")
    val capturedAtMs: Long,

    /** Running count of upload attempts; used to surface in UI / future back-off tuning */
    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0
)
