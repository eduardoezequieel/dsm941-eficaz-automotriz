package com.eficazautomotriz.app.ui.staff

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.AppointmentRow
import com.eficazautomotriz.app.viewmodel.StaffOrderRow
import com.eficazautomotriz.app.viewmodel.staff.OrderAction
import com.eficazautomotriz.app.viewmodel.staff.StaffPanel
import com.eficazautomotriz.app.viewmodel.staff.StaffPanelCounts
import com.eficazautomotriz.app.viewmodel.staff.StaffPanelViewModel
import com.eficazautomotriz.app.viewmodel.staff.canDecideAppointment
import com.eficazautomotriz.app.viewmodel.staff.confirmedAgenda
import com.eficazautomotriz.app.viewmodel.staff.groupStaffServices
import com.eficazautomotriz.app.viewmodel.staff.orderActionsFor
import com.eficazautomotriz.app.viewmodel.staff.panelInProgressOrders
import java.time.LocalDate

private val segments = listOf("Citas", "Servicios", "Reportes")
private const val SEGMENT_APPOINTMENTS = 0
private const val SEGMENT_SERVICES = 1
private const val SEGMENT_REPORTS = 2

/**
 * P8 (f8): three counters, the Citas / Servicios / Reportes segments, pending requests with
 * Confirmar / Reprogramar / Rechazar, in-progress orders and the "Reportes del mes" card.
 * Confirmed appointments stay below that card (f8 does not show them in the first fold).
 * The "Reportes" segment opens the reports tab ([onReports]).
 */
@Composable
fun StaffPanelScreen(
    onAppointment: (String) -> Unit,
    onOrder: (String) -> Unit,
    onNewOrder: () -> Unit,
    onReschedule: (String) -> Unit,
    onEvidence: (String) -> Unit,
    onComplete: (String) -> Unit,
    onReports: () -> Unit,
    vm: StaffPanelViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var segment by rememberSaveable { mutableIntStateOf(SEGMENT_APPOINTMENTS) }
    var rejectingId by rememberSaveable { mutableStateOf<String?>(null) }
    ReloadOnResume(vm::load)
    MessageSnackbar(message, vm::consumeMessage)

    Box(Modifier.fillMaxSize()) {
        val ready = (state as? UiState.Ready)?.data
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, bottom = EficazDimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
        ) {
            item(key = "counters") { CounterRow(ready?.counts) }
            item(key = "segments") {
                SegmentedTabs(
                    options = segments,
                    selectedIndex = segment,
                    onSelect = { index -> if (index == SEGMENT_REPORTS) onReports() else segment = index },
                )
            }
            when (val current = state) {
                UiState.Loading -> item(key = "loading") { LoadingBox(Modifier.padding(EficazDimens.SpaceXl)) }
                is UiState.Error -> item(key = "error") { ErrorBox(current.message, onRetry = vm::load) }
                is UiState.Ready -> if (segment == SEGMENT_SERVICES) {
                    servicesSegment(current.data, onOrder)
                } else {
                    appointmentsSegment(
                        panel = current.data,
                        busy = busy,
                        onAppointment = onAppointment,
                        onConfirm = vm::confirm,
                        onReschedule = onReschedule,
                        onReject = { rejectingId = it },
                        onOrder = onOrder,
                        onEvidence = onEvidence,
                        onStart = vm::startOrder,
                        onComplete = onComplete,
                    )
                    item(key = "reports") { MonthReportsCard(onReports) }
                    confirmedAgendaItems(current.data, onAppointment)
                }
            }
            if (segment == SEGMENT_SERVICES) {
                item(key = "fab-space") {
                    Spacer(Modifier.height(EficazDimens.FabSize + EficazDimens.SpaceLg))
                }
            }
        }
        if (segment == SEGMENT_SERVICES) {
            OrangeFab(
                onClick = onNewOrder,
                contentDescription = "Nueva orden",
                modifier = Modifier.align(Alignment.BottomEnd).padding(EficazDimens.ScreenPadding),
            )
        }
    }
    rejectingId?.let { id ->
        RejectDialog(
            onReject = { reason -> if (vm.reject(id, reason)) rejectingId = null },
            onDismiss = { rejectingId = null },
        )
    }
}

@Composable
private fun CounterRow(counts: StaffPanelCounts?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        CounterTile("${counts?.appointmentsToday ?: 0}", "Citas hoy", Modifier.weight(1f))
        CounterTile("${counts?.pendingConfirmation ?: 0}", "Por confirmar", Modifier.weight(1f))
        CounterTile("${counts?.inProgress ?: 0}", "En proceso", Modifier.weight(1f))
    }
}

private fun LazyListScope.appointmentsSegment(
    panel: StaffPanel,
    busy: Boolean,
    onAppointment: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onReject: (String) -> Unit,
    onOrder: (String) -> Unit,
    onEvidence: (String) -> Unit,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    item(key = "label-pending") { SectionLabel("Solicitudes pendientes", Modifier.padding(top = EficazDimens.SpaceXs)) }
    if (panel.pending.isEmpty()) {
        item(key = "empty-pending") { MutedText("No hay solicitudes pendientes.") }
    }
    items(panel.pending, key = { "pending-${it.appointment.id}" }) { row ->
        PendingRequestCard(row, busy, onAppointment, onConfirm, onReschedule, onReject)
    }

    val active = panelInProgressOrders(panel.orders)
    item(key = "label-active") { SectionLabel("Servicios en proceso", Modifier.padding(top = EficazDimens.SpaceXs)) }
    if (active.isEmpty()) {
        item(key = "empty-active") { MutedText("No hay servicios en proceso.") }
    }
    items(active, key = { "active-${it.order.id}" }) { row ->
        ActiveOrderCard(row, panel.today, busy, onOrder, onEvidence, onStart, onComplete)
    }
}

/**
 * Confirmed appointments from today on. f8 has no such section in the first fold; it stays
 * under "Reportes del mes" so the appointment detail (and the order created from it) remains reachable.
 */
private fun LazyListScope.confirmedAgendaItems(panel: StaffPanel, onAppointment: (String) -> Unit) {
    val confirmed = confirmedAgenda(panel.pending, panel.agenda)
    if (confirmed.isEmpty()) return
    item(key = "label-agenda") { SectionLabel("Agenda confirmada", Modifier.padding(top = EficazDimens.SpaceXs)) }
    items(confirmed, key = { "agenda-${it.appointment.id}" }) { row ->
        StaffAppointmentCard(row, onClick = { onAppointment(row.appointment.id) })
    }
}

private fun LazyListScope.servicesSegment(panel: StaffPanel, onOrder: (String) -> Unit) {
    val groups = groupStaffServices(panel.orders)
    if (groups.isEmpty) {
        item(key = "empty-orders") {
            MutedText("Aún no hay órdenes de servicio. Use + para registrar la recepción de un vehículo.")
        }
        return
    }
    staffOrderSection("En proceso", groups.inProgress, panel.today, onOrder)
    staffOrderSection("Recibidos", groups.received, panel.today, onOrder)
    staffOrderSection("Finalizados", groups.completed, panel.today, onOrder)
}

/** One labelled group of order cards; shared by the panel's Servicios segment and the Servicios tab. */
internal fun LazyListScope.staffOrderSection(
    title: String,
    rows: List<StaffOrderRow>,
    today: LocalDate,
    onOrder: (String) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "label-$title") { SectionLabel(title, Modifier.padding(top = EficazDimens.SpaceXs)) }
    items(rows, key = { "order-${it.order.id}" }) { row ->
        StaffOrderCard(row, today, onClick = { onOrder(row.order.id) })
    }
}

@Composable
private fun PendingRequestCard(
    row: AppointmentRow,
    busy: Boolean,
    onAppointment: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    val id = row.appointment.id
    val decidable = canDecideAppointment(row.appointment.status)
    StaffAppointmentCard(
        row = row,
        onClick = { onAppointment(id) },
        actions = if (decidable) {
            {
                CardActionButton("Confirmar", CardActionStyle.Primary, { onConfirm(id) }, Modifier.weight(1f), enabled = !busy)
                CardActionButton("Reprogramar", CardActionStyle.Outlined, { onReschedule(id) }, Modifier.weight(1f), enabled = !busy)
                CardActionButton("Rechazar", CardActionStyle.Danger, { onReject(id) }, Modifier.weight(1f), enabled = !busy)
            }
        } else {
            null
        },
    )
}

@Composable
private fun ActiveOrderCard(
    row: StaffOrderRow,
    today: LocalDate,
    busy: Boolean,
    onOrder: (String) -> Unit,
    onEvidence: (String) -> Unit,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
) {
    val id = row.order.id
    val actions = orderActionsFor(row.order.status)
    StaffOrderCard(row, today, onClick = { onOrder(id) }) {
        if (OrderAction.ADD_EVIDENCE in actions) {
            CardActionButton("Subir evidencia", CardActionStyle.Outlined, { onEvidence(id) }, Modifier.weight(1f), enabled = !busy)
        }
        if (OrderAction.START in actions) {
            CardActionButton("Iniciar", CardActionStyle.Primary, { onStart(id) }, Modifier.weight(1f), enabled = !busy)
        }
        if (OrderAction.COMPLETE in actions) {
            CardActionButton("Finalizar", CardActionStyle.Primary, { onComplete(id) }, Modifier.weight(1f), enabled = !busy)
        }
    }
}

@Composable
private fun MonthReportsCard(onOpen: () -> Unit) {
    EficazCard(Modifier.padding(top = EficazDimens.SpaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
                Text("Reportes del mes", style = MaterialTheme.typography.titleMedium, color = EficazColors.TextPrimary)
                Text(
                    "Citas por estado y servicios por tipo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EficazColors.TextSecondary,
                )
            }
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            OutlinedNavyButton("Abrir", onClick = onOpen, compact = true)
        }
    }
}

internal fun vehicleLabel(row: AppointmentRow): String = StaffFormats.vehicleLine(row.vehicle, row.appointment.vehicleId)
