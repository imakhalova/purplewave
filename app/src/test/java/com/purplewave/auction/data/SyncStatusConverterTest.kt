package com.purplewave.auction.data

import com.purplewave.auction.data.local.SyncStatusConverter
import com.purplewave.auction.domain.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusConverterTest {

    private val converter = SyncStatusConverter()

    @Test
    fun `round-trips all SyncStatus values`() {
        SyncStatus.values().forEach { status ->
            val string = converter.fromSyncStatus(status)
            val restored = converter.toSyncStatus(string)
            assertEquals("Round-trip failed for $status", status, restored)
        }
    }

    @Test
    fun `stores as uppercase name`() {
        assertEquals("PENDING", converter.fromSyncStatus(SyncStatus.PENDING))
        assertEquals("UPLOADING", converter.fromSyncStatus(SyncStatus.UPLOADING))
        assertEquals("SYNCED", converter.fromSyncStatus(SyncStatus.SYNCED))
        assertEquals("FAILED", converter.fromSyncStatus(SyncStatus.FAILED))
    }
}
