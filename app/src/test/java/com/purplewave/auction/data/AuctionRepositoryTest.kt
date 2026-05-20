package com.purplewave.auction.data

import com.purplewave.auction.data.local.AuctionItemDao
import com.purplewave.auction.data.local.AuctionItemEntity
import com.purplewave.auction.data.remote.AuctionApi
import com.purplewave.auction.data.remote.UploadRequest
import com.purplewave.auction.data.remote.UploadResponse
import com.purplewave.auction.domain.AuctionItem
import com.purplewave.auction.domain.SyncStatus
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuctionRepositoryTest {

    private lateinit var dao: AuctionItemDao
    private lateinit var api: AuctionApi
    private lateinit var repository: AuctionRepository

    private val baseItem = AuctionItem(
        id = 1L,
        title = "Test Tractor",
        description = "Great shape",
        conditionRating = 4,
        photoUri = null,
        syncStatus = SyncStatus.PENDING,
        capturedAtMs = 1_000_000L,
        syncAttempts = 0
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        api = mockk()
        // observeAll needs a flow even though we don't exercise it in these tests
        every { dao.observeAll() } returns flowOf(emptyList())
        repository = AuctionRepository(dao, api)
    }

    @Test
    fun `uploadItem marks UPLOADING then SYNCED on success`() = runTest {
        coEvery { api.uploadItem(any()) } returns
            Response.success(UploadResponse(success = true, serverId = "srv-1"))

        repository.uploadItem(baseItem)

        // UPLOADING must come before SYNCED
        coVerifyOrder {
            dao.updateSyncState(1L, SyncStatus.UPLOADING, 1)
            dao.updateSyncState(1L, SyncStatus.SYNCED, 1)
        }
    }

    @Test
    fun `uploadItem marks UPLOADING then FAILED on HTTP error`() = runTest {
        coEvery { api.uploadItem(any()) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, ""))

        repository.uploadItem(baseItem)

        coVerifyOrder {
            dao.updateSyncState(1L, SyncStatus.UPLOADING, 1)
            dao.updateSyncState(1L, SyncStatus.FAILED, 1)
        }
    }

    @Test
    fun `uploadItem marks FAILED when exception thrown`() = runTest {
        coEvery { api.uploadItem(any()) } throws RuntimeException("network gone")

        repository.uploadItem(baseItem)

        coVerify { dao.updateSyncState(1L, SyncStatus.FAILED, 1) }
    }

    @Test
    fun `uploadItem increments attempt count`() = runTest {
        val itemWithPriorAttempts = baseItem.copy(syncAttempts = 2, syncStatus = SyncStatus.FAILED)
        coEvery { api.uploadItem(any()) } returns
            Response.success(UploadResponse(success = true, serverId = "srv-2"))

        repository.uploadItem(itemWithPriorAttempts)

        coVerify { dao.updateSyncState(1L, SyncStatus.SYNCED, 3) }
    }

    @Test
    fun `uploadItem returns true on success`() = runTest {
        coEvery { api.uploadItem(any()) } returns
            Response.success(UploadResponse(success = true, serverId = "srv-3"))

        val result = repository.uploadItem(baseItem)
        assertTrue(result)
    }

    @Test
    fun `uploadItem returns false on failure`() = runTest {
        coEvery { api.uploadItem(any()) } returns
            Response.error(500, okhttp3.ResponseBody.create(null, ""))

        val result = repository.uploadItem(baseItem)
        assertFalse(result)
    }

    @Test
    fun `getPendingItems delegates to dao getPendingOrFailed`() = runTest {
        val entity = AuctionItemEntity(
            id = 2L, title = "Combine", description = "", conditionRating = 3,
            photoUri = null, syncStatus = SyncStatus.FAILED,
            capturedAtMs = 2_000_000L, syncAttempts = 1
        )
        coEvery { dao.getPendingOrFailed() } returns listOf(entity)

        val result = repository.getPendingItems()

        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
        assertEquals(SyncStatus.FAILED, result.first().syncStatus)
    }
}
