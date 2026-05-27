package com.purplewave.auction.di

import android.content.Context
import androidx.room.Room
import com.purplewave.auction.data.AuctionRepository
import com.purplewave.auction.data.local.AuctionDatabase
import com.purplewave.auction.data.remote.FakeAuctionApi

/**
 * Manual service locator — chosen over Hilt to keep the project self-contained
 * within the 3-hour scope. Swapping to Hilt would be straightforward: replace
 * this object with @Module / @Provides annotations and inject via @HiltViewModel.
 *
 * All properties are lazy so nothing is built until first access.
 */
object ServiceLocator {

    @Volatile
    private var database: AuctionDatabase? = null

    @Volatile
    private var repository: AuctionRepository? = null

    fun provideRepository(context: Context): AuctionRepository {
        return repository ?: synchronized(this) {
            repository ?: buildRepository(context).also { repository = it }
        }
    }

    private fun buildRepository(context: Context): AuctionRepository {
        val db = database ?: Room.databaseBuilder(
            context.applicationContext,
            AuctionDatabase::class.java,
            "auction.db"
        )
            .fallbackToDestructiveMigration()
            .build().also { database = it }

        return AuctionRepository(
            dao = db.auctionItemDao(),
            api = FakeAuctionApi()
        )
    }

    /** Exposed for tests so they can inject a test database */
    fun setRepositoryForTesting(repo: AuctionRepository) {
        repository = repo
    }

    fun reset() {
        database?.close()
        database = null
        repository = null
    }
}
