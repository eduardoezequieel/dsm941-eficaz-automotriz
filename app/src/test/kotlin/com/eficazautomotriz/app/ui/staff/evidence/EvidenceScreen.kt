package com.eficazautomotriz.app.ui.staff.evidence

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.CardShape
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PlaceholderThumb
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.components.evidenceStageLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.staff.EvidenceViewModel
import com.eficazautomotriz.domain.model.enums.EvidenceStage

/**
 * Evidence capture (plan 5.1). The camera writes to a FileProvider Uri; the manifest does not
 * declare CAMERA, so `TakePicture` needs no runtime permission. The gallery uses the system
 * photo picker, which needs no storage permission either.
 */
@Composable
fun EvidenceScreen(vm: EvidenceViewModel = viewModel(factory = LocalVmFactory.current)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    MessageSnackbar(state.message, vm::consumeMessage)

    // Survives the activity being recreated while the camera app is in front.
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) vm.onImage(pendingCameraUri) else vm.discardCapture(pendingCameraUri)
        pendingCameraUri = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> vm.onImage(uri) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
    ) {
        if (!state.cloudinaryConfigured) {
            InfoBanner(
                "Cloudinary no configurado: agregue cloudinary.cloudName y cloudinary.uploadPreset en local.properties y vuelva a compilar.",
                tone = StatusTone.Warning,
            )
        }
        SectionLabel("Etapa")
        SegmentedTabs(
            options = EvidenceStage.entries.map(::evidenceStageLabel),
            selectedIndex = EvidenceStage.entries.indexOf(state.stage),
            onSelect = { vm.selectStage(EvidenceStage.entries[it]) },
            enabled = !state.uploading,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            OutlinedNavyButton(
                "Cámara",
                onClick = {
                    val uri = newCameraUri(context)
                    pendingCameraUri = uri
                    // The camera feature is optional in the manifest: a device may have no camera app.
                    try {
                        camera.launch(uri)
                    } catch (e: ActivityNotFoundException) {
                        pendingCameraUri = null
                        vm.showMessage("No hay una aplicación de cámara disponible; use la galería.")
                    }
                },
                enabled = !state.uploading,
                modifier = Modifier.weight(1f),
            )
            OutlinedNavyButton(
                "Galería",
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !state.uploading,
                modifier = Modifier.weight(1f),
            )
        }
        state.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Vista previa",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(CardShape),
            )
        } ?: PlaceholderThumb("Sin imagen seleccionada", Modifier.fillMaxWidth().height(112.dp))
        PrimaryButton(
            "Subir evidencia",
            onClick = vm::upload,
            enabled = state.canUpload,
            busy = state.uploading,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Evidencias de la orden", Modifier.padding(top = EficazDimens.SpaceXs))
        state.loadError?.let { FieldError(it) }
        if (state.uploaded.isEmpty()) {
            Text("Aún no hay evidencias.", style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
        } else {
            EficazCard(contentPadding = PaddingValues(vertical = EficazDimens.SpaceXs)) {
                state.uploaded.forEachIndexed { index, evidence ->
                    if (index > 0) HorizontalDivider(color = EficazColors.CardBorder)
                    Row(Modifier.fillMaxWidth().padding(EficazDimens.SpaceSm), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = evidence.imageUrl,
                            contentDescription = "Evidencia ${evidenceStageLabel(evidence.stage)}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(EficazDimens.ThumbRadius)),
                        )
                        Column(Modifier.padding(start = EficazDimens.SpaceSm)) {
                            Text(
                                evidenceStageLabel(evidence.stage),
                                style = MaterialTheme.typography.titleSmall,
                                color = EficazColors.TextPrimary,
                            )
                            Text(
                                Formats.dateTime(evidence.uploadedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EficazColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}