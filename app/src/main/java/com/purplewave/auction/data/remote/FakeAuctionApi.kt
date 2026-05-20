package com.purplewave.auction.data.remote

import kotlinx.coroutines.delay
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake implementation of [AuctionApi].
 *
 * Rules (per exercise spec):
 *  • Every 3rd upload attempt fails with an HTTP 500.
 *  • Successful uploads include a simulated 800 ms network delay.
 *  • Failed items are retried on the next sync trigger (handled by the Worker).
 *
 * [attemptCounter] is an AtomicInteger so concurrent Workers don't corrupt the
 * count (though WorkManager serialises our work by tag in practice).
 */
class FakeAuctionApi : AuctionApi {

    private val attemptCounter = AtomicInteger(0)

    override suspend fun uploadItem(request: UploadRequest): Response<UploadResponse> {
        val attempt = attemptCounter.incrementAndGet()

        return if (attempt % 3 == 0) {
            // Simulate a failed request — no delay, server error immediately
            Response.error(500, okhttp3.ResponseBody.create(null, "Internal Server Error"))
        } else {
            delay(SIMULATED_DELAY_MS)
            Response.success(UploadResponse(success = true, serverId = UUID.randomUUID().toString()))
        }
    }

    companion object {
        private const val SIMULATED_DELAY_MS = 800L
    }
}
