package com.purplewave.auction.domain

/**
 * Domain model for an auction equipment item.
 * Kept separate from the Room entity to isolate persistence concerns.
 */
data class AuctionItem(
    val id: Long = 0,
    val title: String,
    val description: String,
    val conditionRating: Int,           // 1–5
    val photoUri: String?,              // null = placeholder
    val syncStatus: SyncStatus,
    val capturedAtMs: Long,
    val syncAttempts: Int = 0
)
