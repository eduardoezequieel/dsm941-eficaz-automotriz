package com.eficazautomotriz.app.ui.staff.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.staff.MutedText
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.viewmodel.staff.CatalogEditor
import com.eficazautomotriz.app.viewmodel.staff.CatalogKind
import com.eficazautomotriz.app.viewmodel.staff.CatalogViewModel
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.validation.TextFields

/** Maintenance intervals: active switch, tap to edit, "Agregar" opens the same dialog. */
@Composable
fun MaintenanceTypesTab(vm: CatalogViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(current.message, onRetry = vm::load)
            is UiState.Ready -> if (current.data.maintenance.isEmpty()) {
                MutedText("Catálogo de mantenimiento no configurado.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
                ) {
                    items(current.data.maintenance, key = { it.id }) { type ->
                        MaintenanceTypeCard(
                            type = type,
                            enabled = type.id !in busy,
                            onEdit = { vm.beginEditMaintenance(type) },
                            onActive = { vm.setMaintenanceActive(type, it) },
                        )
                    }
                }
            }
        }
        OrangeFab(
            onClick = vm::beginCreateMaintenance,
            contentDescription = "Agregar",
            modifier = Modifier.align(Alignment.BottomEnd).padding(EficazDimens.ScreenPadding),
        )
    }
    val form = editor
    if (form != null && form.kind == CatalogKind.MAINTENANCE) {
        MaintenanceTypeDialog(form, saving = vm.isSaving(busy), vm = vm)
    }
}

@Composable
private fun MaintenanceTypeCard(
    type: MaintenanceType,
    enabled: Boolean,
    onEdit: () -> Unit,
    onActive: (Boolean) -> Unit,
) {
    val detail = "Cada ${Formats.km(type.intervalKm)}"
    EficazCard(contentPadding = PaddingValues(start = EficazDimens.CardPadding, end = EficazDimens.SpaceSm, top = EficazDimens.SpaceXs, bottom = EficazDimens.SpaceXs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable(onClick = onEdit).padding(vertical = EficazDimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs),
            ) {
                Text(
                    type.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (type.active) EficazColors.TextPrimary else EficazColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!type.active) StatusChip("Inactivo", StatusTone.Neutral, Modifier.padding(top = EficazDimens.SpaceXs))
            }
            Switch(checked = type.active, onCheckedChange = onActive, enabled = enabled)
        }
    }
}

@Composable
private fun MaintenanceTypeDialog(form: CatalogEditor, saving: Boolean, vm: CatalogViewModel) {
    AlertDialog(
        onDismissRequest = { if (!saving) vm.dismissEditor() },
        containerColor = EficazColors.Surface,
        title = { Text(if (form.id == null) "Nuevo mantenimiento" else "Editar mantenimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                CatalogField(
                    "Nombre", form.name, vm::onNameChange, form.errors["name"], enabled = !saving,
                    maxLength = TextFields.CATALOG_NAME.maxLength,
                    inputFilter = InputFilters.NameCharacters,
                )
                CatalogField(
                    "Intervalo (km)",
                    form.intervalKm,
                    vm::onIntervalChange,
                    form.errors["intervalKm"],
                    enabled = !saving,
                    keyboardType = KeyboardType.Number,
                    hint = "Kilómetros entre servicios.",
                    maxLength = vm.intervalMaxLength,
                    inputFilter = InputFilters.DigitsWithThousands,
                )
                ActiveSwitch(form.active, enabled = !saving, onChange = vm::onActiveChange)
            }
        },
        confirmButton = {
            TextButton(onClick = vm::save, enabled = !saving) {
                Text("Guardar", color = EficazColors.Navy, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissEditor, enabled = !saving) { Text("Cancelar", color = EficazColors.TextSecondary) }
        },
    )
}
