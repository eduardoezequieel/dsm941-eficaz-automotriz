package com.eficazautomotriz.app.ui.client

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.FieldError
import com.eficazautomotriz.app.ui.components.InfoBanner
import com.eficazautomotriz.app.ui.components.KeyValueRow
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.PrimaryButton
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.app.viewmodel.AppointmentStep
import com.eficazautomotriz.app.viewmodel.NewAppointmentState
import com.eficazautomotriz.app.viewmodel.NewAppointmentViewModel
import com.eficazautomotriz.app.viewmodel.SlotOption
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.AppointmentStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * P6 new appointment (f6), on one page: header with the 4-segment progress, a summary card
 * whose VEHÍCULO / SERVICIO rows open inline pickers ("Cambiar"), the month calendar, the slot
 * grid, the request summary and the fixed "Solicitar cita". Validation stays in the ViewModel.
 */
@Composable
fun NewAppointmentScreen(
    onNewVehicle: () -> Unit,
    onViewAppointments: () -> Unit,
    vm: NewAppointmentViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val (step, stepName) = state.progress
    ScreenScaffold(header = { StepHeader("Nueva cita", "Paso $step de 4 · $stepName", step) }) {
        when {
            state.loading -> LoadingBox()
            state.loadError != null -> ErrorBox(state.loadError!!, onRetry = vm::load)
            state.catalogMissing -> ErrorBox(
                "Catálogo no configurado: el taller aún no cargó los tipos de servicio y horarios. Intente más tarde.",
                onRetry = vm::load,
            )
            state.step == AppointmentStep.DONE -> DoneContent(state, vm::startOver, onViewAppointments)
            state.vehicles.isEmpty() -> Column(Modifier.padding(screenPadding()), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd)) {
                EmptyCard("Primero registre un vehículo para poder solicitar una cita.")
                PrimaryButton("Agregar vehículo", onClick = onNewVehicle, modifier = Modifier.fillMaxWidth())
            }
            else -> RequestForm(state, vm)
        }
    }
}

@Composable
private fun RequestForm(state: NewAppointmentState, vm: NewAppointmentViewModel) {
    var editingVehicle by rememberSaveable { mutableStateOf(false) }
    var editingService by rememberSaveable { mutableStateOf(false) }
    val vehicleOpen = state.vehicle == null || editingVehicle
    val serviceOpen = !vehicleOpen && (state.serviceType == null || editingService)
    BackHandler(enabled = editingVehicle || editingService) {
        editingVehicle = false
        editingService = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(screenPadding()),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
        ) {
            state.message?.let { InfoBanner(it, tone = StatusTone.Danger) }
            ChoicesCard(
                state = state,
                vehicleOpen = vehicleOpen,
                serviceOpen = serviceOpen,
                onChangeVehicle = { editingVehicle = !editingVehicle; editingService = false },
                onChangeService = { editingService = !editingService; editingVehicle = false },
                onVehicle = { vm.selectVehicle(it); editingVehicle = false },
                onService = { vm.selectServiceType(it); editingService = false },
            )
            if (state.vehicle != null && state.serviceType != null) {
                SectionHeader("Seleccione la fecha")
                MonthCalendar(
                    today = state.today,
                    lastDate = state.lastDate,
                    selected = state.date,
                    isEnabled = vm::isSelectableDay,
                    onSelect = vm::selectDate,
                    note = closedDaysNote(state.serviceDays),
                )
                state.dateError?.let { FieldError(it) }
                SectionHeader("Franjas horarias disponibles", trailing = state.date?.let(ClientText::shortDayMonth))
                SlotsSection(state, vm::selectSlot)
                state.slotError?.let { FieldError(it) }
                InfoBanner(
                    "Las franjas tachadas ya están reservadas. La disponibilidad se valida nuevamente al " +
                        "confirmar para evitar conflictos con otras reservas.",
                )
                RequestSummary(state)
            }
        }
        PrimaryButton(
            "Solicitar cita",
            onClick = vm::submit,
            busy = state.submitting,
            modifier = Modifier.fillMaxWidth().padding(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.SpaceSm, bottom = EficazDimens.ScreenPadding),
        )
    }
}

/** f6 top card: VEHÍCULO and SERVICIO rows with "Cambiar"; an open row lists the options below it. */
@Composable
private fun ChoicesCard(
    state: NewAppointmentState,
    vehicleOpen: Boolean,
    serviceOpen: Boolean,
    onChangeVehicle: () -> Unit,
    onChangeService: () -> Unit,
    onVehicle: (Vehicle) -> Unit,
    onService: (ServiceType) -> Unit,
) {
    EficazCard(contentPadding = PaddingValues(0.dp)) {
        ChoiceRow(
            label = "Vehículo",
            value = state.vehicle?.let(ClientText::vehicleLine),
            placeholder = "Seleccione el vehículo",
            canChange = state.vehicle != null,
            open = vehicleOpen,
            onChange = onChangeVehicle,
        )
        if (vehicleOpen) {
            state.vehicles.forEach { vehicle ->
                OptionRow(
                    title = ClientText.vehicleLine(vehicle),
                    detail = Formats.km(vehicle.currentMileage),
                    selected = vehicle.id == state.vehicle?.id,
                    onClick = { onVehicle(vehicle) },
                )
            }
        }
        RowDivider(Modifier.padding(horizontal = EficazDimens.ScreenPadding))
        ChoiceRow(
            label = "Servicio",
            value = state.serviceType?.name,
            placeholder = if (state.vehicle == null) "Primero elija el vehículo" else "Seleccione el servicio",
            canChange = state.serviceType != null && !vehicleOpen,
            open = serviceOpen,
            onChange = onChangeService,
        )
        if (serviceOpen) {
            state.serviceTypes.forEach { type ->
                OptionRow(
                    title = type.name,
                    detail = type.description,
                    selected = type.id == state.serviceType?.id,
                    onClick = { onService(type) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    value: String?,
    placeholder: String,
    canChange: Boolean,
    open: Boolean,
    onChange: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = EficazDimens.ScreenPadding, vertical = EficazDimens.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SectionLabel(label)
            Text(
                value ?: placeholder,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal,
                color = if (value != null) EficazColors.TextPrimary else EficazColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canChange) {
            Text(
                if (open) "Listo" else "Cambiar",
                style = MaterialTheme.typography.labelLarge,
                color = EficazColors.Navy,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onChange).padding(EficazDimens.SpaceSm),
            )
        }
    }
}

/** Option of an open picker: tinted when selected, with a navy check. */
@Composable
private fun OptionRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = EficazDimens.SpaceSm, vertical = EficazDimens.SpaceXxs)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) EficazColors.InfoBg else EficazColors.Background)
            .clickable(onClick = onClick)
            .padding(horizontal = EficazDimens.SpaceMd, vertical = EficazDimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = EficazColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado", tint = EficazColors.Navy)
    }
}

/**
 * f6 month calendar, Monday first. The current month starts at today's week (as in the mockup);
 * days outside the range or on closed weekdays are grayed and cannot be tapped.
 */
@Composable
private fun MonthCalendar(
    today: LocalDate,
    lastDate: LocalDate,
    selected: LocalDate?,
    isEnabled: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
    note: String?,
) {
    val first = YearMonth.from(today)
    val last = YearMonth.from(lastDate)
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(selected ?: today).toString()) }
    val month = YearMonth.parse(monthText)
    EficazCard(contentPadding = PaddingValues(horizontal = EficazDimens.SpaceSm, vertical = EficazDimens.SpaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthText = month.minusMonths(1).toString() }, enabled = month > first) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mes anterior", tint = if (month > first) EficazColors.TextPrimary else EficazColors.FieldBorder)
            }
            Text(
                ClientText.monthYear(month.atDay(1)),
                style = MaterialTheme.typography.titleMedium,
                color = EficazColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { monthText = month.plusMonths(1).toString() }, enabled = month < last) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Mes siguiente", tint = if (month < last) EficazColors.TextPrimary else EficazColors.FieldBorder)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            ClientText.weekInitials.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = EficazColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = EficazDimens.SpaceXs),
                )
            }
        }
        calendarWeeks(month, today).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(40.dp).padding(EficazDimens.SpaceXxs), contentAlignment = Alignment.Center) {
                        if (day != null) DayCell(day, selected = day == selected, enabled = isEnabled(day), onSelect = onSelect)
                    }
                }
            }
        }
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = EficazColors.TextSecondary,
                modifier = Modifier.padding(start = EficazDimens.SpaceXs, end = EficazDimens.SpaceXs, top = EficazDimens.SpaceXs),
            )
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, selected: Boolean, enabled: Boolean, onSelect: (LocalDate) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EficazColors.Navy else EficazColors.Surface)
            .clickable(enabled = enabled) { onSelect(day) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> EficazColors.Surface
                enabled -> EficazColors.TextPrimary
                else -> EficazColors.FieldBorder
            },
        )
    }
}

/**
 * Weeks (Monday first, nulls as padding) shown for [month]. The current month starts at the
 * week holding [today], as f6 does; later months show every week.
 */
internal fun calendarWeeks(month: YearMonth, today: LocalDate): List<List<LocalDate?>> {
    val firstDay = month.atDay(1)
    val start = if (YearMonth.from(today) == month) today else firstDay
    val weekStart = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val weeks = mutableListOf<List<LocalDate?>>()
    var cursor = weekStart
    while (!cursor.isAfter(month.atEndOfMonth())) {
        weeks += (0L until 7L).map { offset -> cursor.plusDays(offset).takeIf { YearMonth.from(it) == month } }
        cursor = cursor.plusWeeks(1)
    }
    return weeks
}

/** Calendar footnote naming the closed weekdays ("Los domingos aparecen deshabilitados…"). */
internal fun closedDaysNote(serviceDays: Set<DayOfWeek>): String? {
    if (serviceDays.isEmpty()) return null
    val names = mapOf(
        DayOfWeek.MONDAY to "lunes", DayOfWeek.TUESDAY to "martes", DayOfWeek.WEDNESDAY to "miércoles",
        DayOfWeek.THURSDAY to "jueves", DayOfWeek.FRIDAY to "viernes", DayOfWeek.SATURDAY to "sábados",
        DayOfWeek.SUNDAY to "domingos",
    )
    val closed = DayOfWeek.entries.filter { it !in serviceDays }.map { names.getValue(it) }
    if (closed.isEmpty()) return null
    val list = if (closed.size == 1) closed.first() else closed.dropLast(1).joinToString(", ") + " y " + closed.last()
    return "Los $list aparecen deshabilitados según los días de atención configurados."
}

/** f6 3-column slot grid: selected navy, free white with a border, taken gray and struck through. */
@Composable
private fun SlotsSection(state: NewAppointmentState, onSelect: (SlotOption) -> Unit) {
    when {
        state.date == null -> EmptyCard("Seleccione una fecha para ver sus franjas.")
        state.slotsLoading -> Box(Modifier.fillMaxWidth().padding(EficazDimens.ScreenPadding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EficazColors.Navy, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
        state.slots.isEmpty() -> EmptyCard("El taller no atiende ese día. Elija otra fecha.")
        else -> Column(verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            state.slots.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
                    row.forEach { option ->
                        SlotPill(option, selected = option.slotId == state.slot?.slotId, onSelect = onSelect, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SlotPill(option: SlotOption, selected: Boolean, onSelect: (SlotOption) -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val taken = !option.selectable
    Box(
        modifier
            .height(EficazDimens.ButtonHeightCompact)
            .clip(shape)
            .background(
                when {
                    selected -> EficazColors.Navy
                    taken -> EficazColors.NeutralPill
                    else -> EficazColors.Surface
                },
            )
            .border(1.dp, if (selected) EficazColors.Navy else if (taken) EficazColors.CardBorder else EficazColors.FieldBorder, shape)
            .clickable(enabled = !taken) { onSelect(option) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ClientText.hour12(option.availability.slot.startTime),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                selected -> EficazColors.Surface
                taken -> EficazColors.TextMuted
                else -> EficazColors.TextPrimary
            },
            textDecoration = if (taken) TextDecoration.LineThrough else null,
        )
    }
}

/** f6 "RESUMEN DE LA SOLICITUD" with the initial "Pendiente" chip. */
@Composable
private fun RequestSummary(state: NewAppointmentState) {
    val slot = state.slot?.availability?.slot
    EficazCard(contentPadding = PaddingValues(horizontal = EficazDimens.CardPadding, vertical = EficazDimens.SpaceMd)) {
        SectionLabel("Resumen de la solicitud")
        Spacer(Modifier.height(EficazDimens.SpaceXs))
        BoundedKeyValue("Vehículo", state.vehicle?.let { Formats.plate(it.plate) } ?: "—")
        BoundedKeyValue("Servicio", state.serviceType?.name ?: "—")
        KeyValueRow(
            "Fecha y franja",
            if (state.date != null && slot != null) "${ClientText.shortDayMonth(state.date)} · ${ClientText.time12(slot.startTime)}" else "—",
        )
        Row(Modifier.fillMaxWidth().padding(vertical = EficazDimens.SpaceXs), verticalAlignment = Alignment.CenterVertically) {
            Text("Estado inicial", style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary, modifier = Modifier.weight(1f))
            StatusChip(AppointmentStatus.PENDING.label, StatusColors.toneForAppointment(AppointmentStatus.PENDING))
        }
    }
}

/** Success after requesting: what was booked and where to follow it. */
@Composable
private fun DoneContent(state: NewAppointmentState, onStartOver: () -> Unit, onViewAppointments: () -> Unit) {
    val created = state.created ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenPadding()),
        verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceMd),
    ) {
        EficazCard(contentPadding = PaddingValues(EficazDimens.CardPadding)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(EficazColors.SuccessBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = EficazColors.SuccessText, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(EficazDimens.SpaceSm))
                Text("¡Cita solicitada!", style = MaterialTheme.typography.titleLarge, color = EficazColors.TextPrimary)
                Spacer(Modifier.height(EficazDimens.SpaceXs))
                Text(
                    "Su cita #${created.id} quedó pendiente de confirmación por el taller.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(EficazDimens.SpaceSm))
            RowDivider()
            Spacer(Modifier.height(EficazDimens.SpaceXs))
            state.vehicle?.let { BoundedKeyValue("Vehículo", ClientText.vehicleLine(it)) }
            state.serviceType?.let { BoundedKeyValue("Servicio", it.name) }
            KeyValueRow(
                "Fecha y franja",
                "${ClientText.shortDate(created.date)} · ${state.slot?.availability?.slot?.let { ClientText.time12(it.startTime) } ?: ""}".trimEnd(' ', '·'),
            )
        }
        PrimaryButton("Ver mis citas", onClick = onViewAppointments, modifier = Modifier.fillMaxWidth())
        OutlinedNavyButton("Solicitar otra cita", onClick = onStartOver, modifier = Modifier.fillMaxWidth())
    }
}
