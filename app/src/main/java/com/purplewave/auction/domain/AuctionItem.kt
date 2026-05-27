package com.purplewave.auction.domain

/**
 * Domain model for an auction equipment item.
 * Kept separate from the Room entity to isolate persistence concerns.
 *
 * ID strategy:
 *  - [id]       — Room auto-increment PK, local to this device only, never sent to the server.
 *  - [clientId] — UUID assigned at capture time. Stable across sync attempts and unique
 *                 across devices, so the server can deduplicate re-uploads of the same item.
 *  - [serverId] — Opaque string assigned by the server on successful upload. Used for
 *                 cross-device reconciliation: when fetching the server list, we match on
 *                 serverId, not the local id, so two devices capturing independent items
 *                 never collide.
 */
data class AuctionItem(
    val id: Long = 0,
    val clientId: String,               // UUID, generated at capture, never changes
    val title: String,
    val description: String,
    val conditionRating: Int,           // 1–5
    val photoUri: String?,              // null = placeholder
    val syncStatus: SyncStatus,
    val capturedAtMs: Long,
    val syncAttempts: Int = 0,
    val serverId: String? = null        // null until server confirms the upload
)
