package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.EmptyState
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.viewmodel.ClientHistoryEntry
import com.eficazautomotriz.app.viewmodel.ClientHistoryViewModel

/** Bottom bar "Historial": services of all the client's vehicles, newest first. */
@Composable
fun ClientHistoryScreen(
    onService: (String) -> Unit,
    vm: ClientHistoryViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReloadOnResume(vm::load)
    val count = (state as? UiState.Ready)?.data?.size
    ScreenScaffold(
        header = {
            AppHeader(
                title = "Historial",
                subtitle = when (count) {
                    null -> "Servicios de sus vehículos"
                    1 -> "1 servicio registrado"
                    else -> "$count servicios registrados"
                },
                showAvatar = false,
            )
        },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> if (s.data.isEmpty()) {
                EmptyState("Sus vehículos aún no tienen servicios registrados.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(EficazDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
                ) {
                    item { SectionLabel("Servicios realizados") }
                    items(s.data, key = { it.row.detail.order.id }) { entry ->
                        HistoryCard(entry, onClick = { onService(entry.row.detail.order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(entry: ClientHistoryEntry, onClick: () -> Unit) {
    val order = entry.row.detail.order
    val vehicle = entry.vehicle
    val date = (order.closedAt ?: order.receivedAt).toLocalDate()
    val details = listOfNotNull(
        "${vehicle.make} ${vehicle.model} ${vehicle.year}",
        Formats.date(date),
        order.mileageAtService?.let(Formats::km),
    ).joinToString(" · ")
    EficazCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXxs)) {
                Text(entry.row.serviceName, style = MaterialTheme.typography.titleSmall, color = EficazColors.TextPrimary)
                Text(details, style = MaterialTheme.typography.bodyMedium, color = EficazColors.TextSecondary)
            }
            Spacer(Modifier.width(EficazDimens.SpaceSm))
            StatusChip(order.status.label, StatusColors.toneForService(order.status))
        }
    }
}
