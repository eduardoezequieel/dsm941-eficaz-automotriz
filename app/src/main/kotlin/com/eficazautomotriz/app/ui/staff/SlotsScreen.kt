package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.InputFilters
import com.eficazautomotriz.app.ui.components.revealWhenFocused
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.staff.SlotRow
import com.eficazautomotriz.app.viewmodel.staff.SlotsViewModel

/** Weekly slot configuration grouped by day. */
@Composable
fun SlotsScreen(vm: SlotsViewModel = viewModel(factory = LocalVmFactory.current)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    MessageSnackbar(message, vm::consumeMessage)

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> if (s.data.isEmpty()) {
                MutedText("Catálogo de franjas no configurado.", Modifier.padding(EficazDimens.ScreenPadding))
            } else {
                LazyColumn(contentPadding = PaddingValues(EficazDimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                    s.data.forEach { day ->
                        item(key = day.day.name) { SectionLabel(Formats.day(day.day), Modifier.padding(top = EficazDimens.SpaceSm)) }
                        items(day.slots, key = { it.slot.id }) { row ->
                            SlotCard(row, enabled = row.slot.id !in busy, maxCapacity = vm.maxCapacity, vm = vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(row: SlotRow, enabled: Boolean, maxCapacity: Int, vm: SlotsViewModel) {
    val slot = row.slot
    var draft by remember(slot.id, slot.capacity) { mutableStateOf(slot.capacity.toString()) }
    EficazCard(contentPadding = PaddingValues(start = EficazDimens.CardPadding, end = EficazDimens.SpaceSm, top = EficazDimens.SpaceSm, bottom = EficazDimens.SpaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    StaffFormats.timeRange12(slot.startTime, slot.endTime),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (slot.enabled) EficazColors.TextPrimary else EficazColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (slot.capacity == 0) "Capacidad: ${row.effectiveCapacity} (por omisión)" else "Capacidad: ${slot.capacity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { vm.changeCapacity(slot, -1) }, enabled = enabled && slot.capacity > 0) {
                Icon(Icons.Filled.Remove, contentDescription = "Reducir capacidad", tint = EficazColors.Navy)
            }
            CapacityField(
                value = draft,
                onValueChange = { draft = it },
                onCommit = {
                    val parsed = draft.toIntOrNull()
                    if (parsed == null) draft = slot.capacity.toString() else vm.setCapacity(slot, parsed)
                },
                enabled = enabled,
                maxCapacity = maxCapacity,
            )
            IconButton(onClick = { vm.changeCapacity(slot, +1) }, enabled = enabled && slot.capacity < maxCapacity) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar capacidad", tint = EficazColors.Navy)
            }
            Switch(checked = slot.enabled, onCheckedChange = { vm.setEnabled(slot, it) }, enabled = enabled)
        }
    }
}

/** Digits only, capped at the configured maximum. Commits when the field loses focus or the user presses done. */
@Composable
private fun CapacityField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    enabled: Boolean,
    maxCapacity: Int,
) {
    val shape = RoundedCornerShape(EficazDimens.FieldRadius)
    var focused by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    val limit = maxCapacity.toString().length
    BasicTextField(
        value = value,
        onValueChange = { typed -> onValueChange(InputFilters.apply(typed, InputFilters.capacity(maxCapacity), limit)) },
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleSmall.copy(
            color = if (enabled) EficazColors.TextPrimary else EficazColors.TextMuted,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(EficazColors.Navy),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit() }),
        modifier = Modifier
            .revealWhenFocused(below = 0.dp)
            .width(EficazDimens.MinTouchTarget)
            .clip(shape)
            .background(EficazColors.Surface)
            .border(EficazDimens.CardBorder, if (focused) EficazColors.Navy else EficazColors.FieldBorder, shape)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) hadFocus = true
                else if (hadFocus) {
                    hadFocus = false
                    onCommit()
                }
            },
        decorationBox = { inner ->
            Box(
                Modifier.heightIn(min = EficazDimens.MinTouchTarget).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { inner() }
        },
    )
}
