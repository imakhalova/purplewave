package com.purplewave.auction.data.remote

import kotlinx.coroutines.delay
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake implementation of [AuctionApi].
 *
 * Upload rules (per exercise spec):
 *  • Every 3rd upload attempt fails with an HTTP 500.
 *  • Successful uploads include a simulated 800 ms network delay.
 *  • Failed items are retried on the next sync trigger (handled by the Worker).
 *
 * Idempotency:
 *  • [uploadedItems] is keyed by [clientId]. Re-uploading the same item (same
 *    clientId) returns the same [serverId], simulating server-side deduplication.
 *    This means a retry after a network timeout that already succeeded won't
 *    create a duplicate record.
 *
 * Fetch:
 *  • [fetchItems] returns all successfully uploaded items, keyed by clientId.
 *    Both IDs are returned so the client can reconcile by serverId and also
 *    detect items uploaded by other devices by clientId.
 */
class FakeAuctionApi : AuctionApi {

    private val attemptCounter = AtomicInteger(0)

    // keyed by clientId for idempotency
    private val uploadedItems = ConcurrentHashMap<String, ServerItem>()

    override suspend fun uploadItem(request: UploadRequest): Response<UploadResponse> {
        val attempt = attemptCounter.incrementAndGet()

        return if (attempt % 3 == 0) {
            Response.error(500, okhttp3.ResponseBody.create(null, "Internal Server Error"))
        } else {
            delay(SIMULATED_DELAY_MS)

            // Return existing serverId on retry (idempotency)
            val item = uploadedItems.getOrPut(request.clientId) {
                ServerItem(
                    serverId = UUID.randomUUID().toString(),
                    clientId = request.clientId,
                    title = request.title,
                    description = request.description,
                    conditionRating = request.conditionRating,
                    capturedAtMs = request.capturedAtMs
                )
            }
            Response.success(UploadResponse(success = true, serverId = item.serverId))
        }
    }

    override suspend fun fetchItems(): Response<List<ServerItem>> {
        delay(SIMULATED_DELAY_MS)
        return Response.success(uploadedItems.values.toList())
    }

    companion object {
        private const val SIMULATED_DELAY_MS = 800L
    }
}
