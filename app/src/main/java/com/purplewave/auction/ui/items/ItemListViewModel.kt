package com.purplewave.auction.ui.items

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.purplewave.auction.data.AuctionRepository
import com.purplewave.auction.di.ServiceLocator
import com.purplewave.auction.domain.AuctionItem
import com.purplewave.auction.domain.SyncStatus
import com.purplewave.auction.worker.SyncWorker
import kotlinx.coroutines.flow.*

data class ItemListUiState(
    val items: List<AuctionItem> = emptyList(),
    val isLoading: Boolean = true
)

class ItemListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuctionRepository =
        ServiceLocator.provideRepository(application)

    val uiState: StateFlow<ItemListUiState> = repository.items
        .map { items -> ItemListUiState(items = items, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemListUiState()
        )

    /**
     * Enqueues a sync. WorkManager deduplicates by tag, so spamming this is safe.
     * The CONNECTED constraint means it will wait automatically if offline.
     */
    fun triggerSync() {
        WorkManager.getInstance(getApplication())
            .enqueue(SyncWorker.buildRequest())
    }
}
