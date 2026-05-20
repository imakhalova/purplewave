package com.purplewave.auction.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.purplewave.auction.data.AuctionRepository
import com.purplewave.auction.di.ServiceLocator
import com.purplewave.auction.domain.AuctionItem
import com.purplewave.auction.domain.SyncStatus
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: AuctionRepository

    private val pendingItem = AuctionItem(
        id = 1L, title = "Tractor", description = "", conditionRating = 3,
        photoUri = null, syncStatus = SyncStatus.PENDING,
        capturedAtMs = 1_000_000L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        ServiceLocator.setRepositoryForTesting(repository)
    }

    @After
    fun tearDown() {
        ServiceLocator.reset()
    }

    @Test
    fun `returns success when no pending items`() = runTest {
        coEvery { repository.getPendingItems() } returns emptyList()

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(Result.success(), result)
    }

    @Test
    fun `calls uploadItem for each pending item`() = runTest {
        val items = listOf(pendingItem, pendingItem.copy(id = 2L))
        coEvery { repository.getPendingItems() } returns items
        coEvery { repository.uploadItem(any()) } returns true

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        worker.doWork()

        coVerify(exactly = 2) { repository.uploadItem(any()) }
    }

    @Test
    fun `returns success even when some uploads fail`() = runTest {
        coEvery { repository.getPendingItems() } returns listOf(pendingItem)
        coEvery { repository.uploadItem(any()) } returns false   // simulate failure

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        // Worker should succeed overall — failed items stay in DB for next sync
        assertEquals(Result.success(), result)
    }

    @Test
    fun `returns retry when unexpected exception escapes`() = runTest {
        coEvery { repository.getPendingItems() } throws RuntimeException("DB exploded")

        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }
}
