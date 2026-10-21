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
import com.eficazautomotriz.app.ui.components.BoxedTextField
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.InputFilter
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.staff.MutedText
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.staff.CatalogEditor
import com.eficazautomotriz.app.viewmodel.staff.CatalogKind
import com.eficazautomotriz.app.viewmodel.staff.CatalogViewModel
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.validation.TextFields

/** Service catalog: active switch, tap to edit, "Agregar" opens the same dialog. */
@Composable
fun ServiceTypesTab(vm: CatalogViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(current.message, onRetry = vm::load)
            is UiState.Ready -> if (current.data.services.isEmpty()) {
                MutedText("Catálogo de servicios no configurado.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
                ) {
                    items(current.data.services, key = { it.id }) { type ->
                        ServiceTypeCard(
                            type = type,
                            enabled = type.id !in busy,
                            onEdit = { vm.beginEditService(type) },
                            onActive = { vm.setServiceActive(type, it) },
                        )
                    }
                }
            }
        }
        OrangeFab(
            onClick = vm::beginCreateService,
            contentDescription = "Agregar",
            modifier = Modifier.align(Alignment.BottomEnd).padding(EficazDimens.ScreenPadding),
        )
    }
    val form = editor
    if (form != null && form.kind == CatalogKind.SERVICE) {
        ServiceTypeDialog(form, saving = vm.isSaving(busy), vm = vm)
    }
}

@Composable
private fun ServiceTypeCard(
    type: ServiceType,
    enabled: Boolean,
    onEdit: () -> Unit,
    onActive: (Boolean) -> Unit,
) {
    val detail = type.description.ifBlank { "Sin descripción" }
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!type.active) StatusChip("Inactivo", StatusTone.Neutral, Modifier.padding(top = EficazDimens.SpaceXs))
            }
            Switch(checked = type.active, onCheckedChange = onActive, enabled = enabled)
        }
    }
}

@Composable
private fun ServiceTypeDialog(form: CatalogEditor, saving: Boolean, vm: CatalogViewModel) {
    AlertDialog(
        onDismissRequest = { if (!saving) vm.dismissEditor() },
        containerColor = EficazColors.Surface,
        title = { Text(if (form.id == null) "Nuevo servicio" else "Editar servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                CatalogField(
                    "Nombre", form.name, vm::onNameChange, form.errors["name"], enabled = !saving,
                    maxLength = TextFields.CATALOG_NAME.maxLength,
                    inputFilter = InputFilters.NameCharacters,
                )
                CatalogField(
                    "Descripción", form.description, vm::onDescriptionChange, form.errors["description"], enabled = !saving,
                    maxLength = TextFields.CATALOG_DESCRIPTION.maxLength,
                    inputFilter = InputFilters.SingleLine,
                    showCounter = true,
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

@Composable
internal fun ActiveSwitch(active: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Activo", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = EficazColors.TextPrimary)
        Switch(checked = active, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Boxed catalog field with its error (or [hint]) below, as in the other staff forms. */
@Composable
internal fun CatalogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null,
    maxLength: Int? = null,
    inputFilter: InputFilter = InputFilters.None,
    showCounter: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXs)) {
        BoxedTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            enabled = enabled,
            keyboardType = keyboardType,
            maxLength = maxLength,
            inputFilter = inputFilter,
            showCounter = showCounter,
        )
        when {
            error != null -> FieldError(error)
            hint != null -> Text(hint, style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary)
        }
    }
}
