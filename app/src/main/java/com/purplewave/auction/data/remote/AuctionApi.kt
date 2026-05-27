package com.purplewave.auction.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Upload payload. [clientId] is a device-generated UUID that acts as an
 * idempotency key — if the same item is uploaded twice (e.g. after a retry
 * following a network timeout where the first attempt actually succeeded),
 * the server can detect the duplicate via clientId and return the same
 * serverId without creating a second record.
 */
data class UploadRequest(
    val clientId: String,
    val title: String,
    val description: String,
    val conditionRating: Int,
    val capturedAtMs: Long
)

/**
 * The server assigns its own opaque [serverId]. This is the canonical identity
 * used for cross-device reconciliation — never the device-local Room auto-increment id.
 */
data class UploadResponse(
    val success: Boolean,
    val serverId: String
)

/**
 * Item as the server knows it. Contains both IDs so the client can match
 * server-list entries back to local rows:
 *  - match on [serverId] when the item was previously uploaded from this device
 *  - match on [clientId] to detect items uploaded by other devices that happen
 *    to share the same physical auction lot (future multi-device dedup use case)
 */
data class ServerItem(
    val serverId: String,
    val clientId: String,
    val title: String,
    val description: String,
    val conditionRating: Int,
    val capturedAtMs: Long
)

/**
 * Retrofit interface that defines the full server contract:
 *  - [uploadItem] pushes a single captured item to the server.
 *  - [fetchItems] retrieves all items the server knows about, used to confirm
 *    previously uploaded items still exist and to reconcile local state.
 */
interface AuctionApi {
    @POST("items/upload")
    suspend fun uploadItem(@Body request: UploadRequest): Response<UploadResponse>

    @GET("items")
    suspend fun fetchItems(): Response<List<ServerItem>>
}
