package com.purplewave.auction.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.purplewave.auction.data.AuctionRepository
import com.purplewave.auction.di.ServiceLocator
import com.purplewave.auction.domain.AuctionItem
import com.purplewave.auction.domain.SyncStatus
import com.purplewave.auction.worker.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CaptureUiState(
    val title: String = "",
    val description: String = "",
    val conditionRating: Int = 3,
    val hasPhoto: Boolean = false,      // true once the user "captures" a photo
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuctionRepository =
        ServiceLocator.provideRepository(application)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, error = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onRatingChange(value: Int) = _uiState.update { it.copy(conditionRating = value) }

    /**
     * Simulates the camera returning a captured image.
     * With real CameraX this would receive a URI; here we just toggle a flag
     * and store a sentinel value that the UI layer maps to the bundled drawable.
     */
    fun onPhotoCapture() = _uiState.update { it.copy(hasPhoto = true) }

    fun saveItem() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.saveItem(
                    AuctionItem(
                        // UUID generated here, on the capturing device, before any server
                        // interaction. Stable across retries; used as an idempotency key
                        // on upload so the server won't create duplicates on retry.
                        clientId = UUID.randomUUID().toString(),
                        title = state.title.trim(),
                        description = state.description.trim(),
                        conditionRating = state.conditionRating,
                        photoUri = if (state.hasPhoto) PHOTO_CAPTURED_SENTINEL else null,
                        syncStatus = SyncStatus.PENDING,
                        capturedAtMs = System.currentTimeMillis()
                    )
                )
                // Trigger a sync immediately after capture; WorkManager holds it if offline
                WorkManager.getInstance(getApplication())
                    .enqueue(SyncWorker.buildRequest())

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = "Failed to save: ${e.message}")
                }
            }
        }
    }

    companion object {
        /**
         * Sentinel stored in Room when the user tapped the photo capture button.
         * In a real build this would be replaced by an actual content:// URI from CameraX.
         * The UI layer checks for this value to decide which image to show.
         */
        const val PHOTO_CAPTURED_SENTINEL = "local://captured_photo"
    }
}
