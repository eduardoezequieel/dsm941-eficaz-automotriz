package com.eficazautomotriz.app.viewmodel.staff

import android.net.Uri
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EvidenceState(
    val stage: EvidenceStage = EvidenceStage.RECEPTION,
    /** Picked or captured image, shown as a preview until it is uploaded. */
    val imageUri: Uri? = null,
    val uploading: Boolean = false,
    val uploaded: List<Evidence> = emptyList(),
    val loadError: String? = null,
    val message: String? = null,
    val cloudinaryConfigured: Boolean = true,
) {
    val canUpload: Boolean get() = imageUri != null && !uploading && cloudinaryConfigured
}

/**
 * Evidence capture (5.1): compress on IO, upload to Cloudinary, then store only the
 * returned URL through `ServiceOrderService.addEvidence`.
 */
class EvidenceViewModel(
    graph: AppGraph,
    private val actor: User,
    private val orderId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(EvidenceState(cloudinaryConfigured = graph.cloudinaryConfigured))
    val state: StateFlow<EvidenceState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(uploaded = outcome.value, loadError = null)
                    is Outcome.Failure -> it.copy(loadError = outcome.error.userMessage())
                }
            }
        }) {
            when (val detail = graph.serviceOrderService.detailOf(actor, orderId)) {
                is Outcome.Success -> Outcome.success(detail.value.evidences.sortedBy { it.uploadedAt })
                is Outcome.Failure -> detail
            }
        }
    }

    fun selectStage(stage: EvidenceStage) = _state.update { it.copy(stage = stage) }

    fun onImage(uri: Uri?) {
        if (uri == null) return
        val previous = _state.value.imageUri
        _state.update { it.copy(imageUri = uri, message = null) }
        // The replaced preview will never be uploaded: drop it if it was a camera capture.
        if (previous != null && previous != uri) graph.discardTempImage(previous)
    }

    /** A cancelled capture may still leave an empty file behind. */
    fun discardCapture(uri: Uri?) {
        if (uri != null) graph.discardTempImage(uri)
    }

    override fun onCleared() {
        _state.value.imageUri?.let(graph::discardTempImage)
    }

    fun upload() {
        val current = _state.value
        val uri = current.imageUri ?: return
        if (!current.canUpload) return
        _state.update { it.copy(uploading = true, message = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(
                        // The temp file was deleted inside the upload block.
                        uploading = false,
                        imageUri = null,
                        uploaded = it.uploaded + outcome.value,
                        message = "Evidencia de ${outcome.value.stage.label.lowercase()} registrada.",
                    )
                    is Outcome.Failure -> it.copy(uploading = false, message = outcome.error.userMessage())
                }
            }
        }) {
            val bytes = graph.compressImage(uri)
            val url = graph.cloudinaryUploader.upload(bytes, "evd_${orderId}_${System.currentTimeMillis()}.jpg")
            graph.serviceOrderService.addEvidence(actor, orderId, current.stage, url, now()).also { outcome ->
                // Once the URL is stored the local capture is no longer needed.
                if (outcome is Outcome.Success) graph.discardTempImage(uri)
            }
        }
    }

    fun showMessage(message: String) = _state.update { it.copy(message = message) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
