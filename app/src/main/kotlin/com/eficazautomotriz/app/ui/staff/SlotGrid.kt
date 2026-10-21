package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.SlotOption

/** Non-lazy slot grid, safe inside scrolling columns; unavailable slots show their reason. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotChips(options: List<SlotOption>, selectedId: String?, onSelect: (SlotOption) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        options.forEach { option ->
            val slot = option.availability.slot
            val selected = selectedId == option.slotId
            OptionPill(selected = selected, onClick = { onSelect(option) }, enabled = option.selectable) {
                OptionText(
                    StaffFormats.timeRange12(slot.startTime, slot.endTime),
                    selected = selected,
                    enabled = option.selectable,
                    secondary = option.unavailableLabel ?: "${option.availability.capacity - option.availability.occupied} cupo(s)",
                )
            }
        }
    }
}
