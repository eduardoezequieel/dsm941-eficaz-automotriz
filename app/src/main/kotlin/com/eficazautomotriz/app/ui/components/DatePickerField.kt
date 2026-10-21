package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.viewmodel.validation.NewAppointmentValidator
import java.time.LocalDate

/**
 * Button that shows a date and opens a Material 3 date picker dialog. The picker works in
 * UTC-midnight milliseconds, so conversions go through [NewAppointmentValidator].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: LocalDate?,
    yearRange: IntRange,
    isSelectable: (LocalDate) -> Boolean,
    onSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Text(if (value == null) label else "$label: ${Formats.longDate(value)}")
    }
    if (!open) return

    val selectable = remember(isSelectable, yearRange) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isSelectable(NewAppointmentValidator.dateFromPickerMillis(utcTimeMillis))

            override fun isSelectableYear(year: Int): Boolean = year in yearRange
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value?.let(NewAppointmentValidator::pickerMillisOf),
        yearRange = yearRange,
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(
                onClick = {
                    open = false
                    state.selectedDateMillis?.let { onSelected(NewAppointmentValidator.dateFromPickerMillis(it)) }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}
