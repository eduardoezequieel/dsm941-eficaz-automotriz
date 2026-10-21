package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.domain.model.Appointment
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

@Composable
private fun ClickableCard(onClick: (() -> Unit)?, modifier: Modifier, content: @Composable () -> Unit) {
    EficazCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(0.dp)) { content() }
}

@Composable
private fun CardBody(title: String, lines: List<String>, badge: @Composable (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(EficazDimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = EficazColors.TextPrimary)
            lines.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
            }
        }
        badge?.invoke()
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    maintenanceStatus: MaintenanceStatus?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    compactMileage: Boolean = false,
) {
    val lines = if (compactMileage) {
        listOf("${vehicle.plate} · ${Formats.km(vehicle.currentMileage)}")
    } else {
        listOf("Placa ${vehicle.plate}", Formats.km(vehicle.currentMileage))
    }
    ClickableCard(onClick, modifier) {
        CardBody(
            title = "${vehicle.make} ${vehicle.model} ${vehicle.year}",
            lines = lines,
            badge = maintenanceStatus?.let { status ->
                { StatusChip(statusLabel ?: maintenanceShortLabel(status), StatusColors.toneForMaintenance(status)) }
            },
        )
    }
}

/** `vehicleLabel`, `serviceName` and `slotLabel` arrive already resolved by the ViewModel. */
@Composable
fun AppointmentCard(
    appointment: Appointment,
    vehicleLabel: String,
    serviceName: String,
    slotLabel: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    clientName: String? = null,
) {
    ClickableCard(onClick, modifier) {
        CardBody(
            title = serviceName,
            lines = listOfNotNull(
                clientName?.let { "Cliente: $it" },
                "${Formats.longDate(appointment.date)} · $slotLabel",
                vehicleLabel,
                appointment.reason?.let { "Motivo: $it" },
            ),
            badge = { StatusChip(appointment.status.label, StatusColors.toneForAppointment(appointment.status)) },
        )
    }
}

@Composable
fun ServiceOrderCard(
    order: ServiceOrder,
    serviceName: String,
    evidenceCount: Int?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    ClickableCard(onClick, modifier) {
        CardBody(
            title = serviceName,
            lines = listOfNotNull(
                subtitle,
                "Recibido ${Formats.dateTime(order.receivedAt)}",
                order.closedAt?.let { "Cerrado ${Formats.dateTime(it)}" },
                evidenceCount?.let { "$it evidencia(s)" },
            ),
            badge = { StatusChip(order.status.label, StatusColors.toneForService(order.status)) },
        )
    }
}
