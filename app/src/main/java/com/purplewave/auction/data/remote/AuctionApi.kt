package com.purplewave.auction.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class UploadRequest(
    val id: Long,
    val title: String,
    val description: String,
    val conditionRating: Int,
    val capturedAtMs: Long
)

data class UploadResponse(
    val success: Boolean,
    val serverId: String
)

/**
 * Retrofit interface that defines the upload contract.
 * The actual Retrofit instance never calls a real network — it is wired to
 * [FakeAuctionApi] via a custom CallAdapter / direct substitution in the DI
 * layer.  Having the interface keeps the contract testable and swap-friendly.
 */
interface AuctionApi {
    @POST("items/upload")
    suspend fun uploadItem(@Body request: UploadRequest): Response<UploadResponse>
}
