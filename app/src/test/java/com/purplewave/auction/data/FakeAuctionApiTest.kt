package com.purplewave.auction.data

import com.purplewave.auction.data.remote.FakeAuctionApi
import com.purplewave.auction.data.remote.UploadRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeAuctionApiTest {

    private lateinit var api: FakeAuctionApi

    @Before
    fun setUp() {
        api = FakeAuctionApi()
    }

    private fun makeRequest(id: Long) = UploadRequest(
        id = id,
        title = "Item $id",
        description = "Desc",
        conditionRating = 3,
        capturedAtMs = System.currentTimeMillis()
    )

    @Test
    fun `first attempt succeeds`() = runTest {
        val response = api.uploadItem(makeRequest(1))
        assertTrue("Expected success on attempt 1", response.isSuccessful)
    }

    @Test
    fun `second attempt succeeds`() = runTest {
        api.uploadItem(makeRequest(1))
        val response = api.uploadItem(makeRequest(2))
        assertTrue("Expected success on attempt 2", response.isSuccessful)
    }

    @Test
    fun `third attempt fails`() = runTest {
        api.uploadItem(makeRequest(1))
        api.uploadItem(makeRequest(2))
        val response = api.uploadItem(makeRequest(3))
        assertFalse("Expected failure on attempt 3", response.isSuccessful)
        assertEquals(500, response.code())
    }

    @Test
    fun `fourth attempt succeeds again`() = runTest {
        repeat(3) { api.uploadItem(makeRequest(it.toLong())) }
        val response = api.uploadItem(makeRequest(4))
        assertTrue("Expected success on attempt 4", response.isSuccessful)
    }

    @Test
    fun `sixth attempt fails`() = runTest {
        repeat(5) { api.uploadItem(makeRequest(it.toLong())) }
        val response = api.uploadItem(makeRequest(6))
        assertFalse("Expected failure on attempt 6 (every 3rd)", response.isSuccessful)
    }

    @Test
    fun `successful response contains serverId`() = runTest {
        val response = api.uploadItem(makeRequest(1))
        val body = response.body()
        assertNotNull(body)
        assertTrue(body!!.success)
        assertTrue("serverId should be a UUID", body.serverId.isNotBlank())
    }
}
