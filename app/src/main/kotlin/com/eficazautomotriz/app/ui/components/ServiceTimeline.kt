package com.eficazautomotriz.app.ui.components

import com.eficazautomotriz.app.ui.theme.EficazDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.ServiceStatus

/** Recibido / En proceso / Finalizado. A filled dot means that step was reached. */
@Composable
fun ServiceStatusTimeline(order: ServiceOrder, modifier: Modifier = Modifier) {
    val steps = listOf(
        TimelineStep("Recibido", reached = true, detail = Formats.dateTime(order.receivedAt)),
        TimelineStep(
            title = "En proceso",
            reached = order.status != ServiceStatus.RECEIVED,
            detail = order.startedAt?.let(Formats::dateTime),
        ),
        TimelineStep(
            title = "Finalizado",
            reached = order.status == ServiceStatus.COMPLETED,
            detail = order.closedAt?.let { "${Formats.dateTime(it)} · agregado al historial" },
        ),
    )
    Card(modifier) {
        Column(Modifier.padding(EficazDimens.CardPadding)) {
            steps.forEachIndexed { index, step ->
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimelineDot(step.reached)
                        if (index < steps.lastIndex) {
                            Box(
                                Modifier
                                    .padding(vertical = 2.dp)
                                    .size(width = 2.dp, height = 28.dp)
                                    .background(if (step.reached) StatusColors.UpToDate else StatusColors.NoPreviousRecord),
                            )
                        }
                    }
                    Column(Modifier.padding(start = 12.dp, bottom = if (index == steps.lastIndex) 0.dp else 8.dp)) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        step.detail?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class TimelineStep(val title: String, val reached: Boolean, val detail: String?)

@Composable
private fun TimelineDot(reached: Boolean) {
    val color = if (reached) StatusColors.UpToDate else StatusColors.NoPreviousRecord
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .then(
                if (reached) Modifier.background(color)
                else Modifier.border(2.dp, color, CircleShape),
            ),
    )
}
