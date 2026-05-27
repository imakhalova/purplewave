package com.purplewave.auction.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AuctionItemEntity::class],
    version = 2,
    exportSchema = true          // keeps schema JSON for migration audits
)
@TypeConverters(SyncStatusConverter::class)
abstract class AuctionDatabase : RoomDatabase() {
    abstract fun auctionItemDao(): AuctionItemDao
}
