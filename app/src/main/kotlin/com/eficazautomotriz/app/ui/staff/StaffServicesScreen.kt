package com.eficazautomotriz.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.ErrorBox
import com.eficazautomotriz.app.ui.components.LoadingBox
import com.eficazautomotriz.app.ui.components.OrangeFab
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.staff.StaffServicesViewModel
import java.time.LocalDate

/** Bottom bar "Servicios" (staff): all service orders grouped by status, plus "Nueva orden". */
@Composable
fun StaffServicesScreen(
    onOrder: (String) -> Unit,
    onNewOrder: () -> Unit,
    vm: StaffServicesViewModel = viewModel(factory = LocalVmFactory.current),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReloadOnResume(vm::load)
    val today = remember { LocalDate.now() }
    val inProgress = (state as? UiState.Ready)?.data?.inProgress?.size
    ScreenScaffold(
        header = {
            AppHeader(
                title = "Servicios",
                subtitle = inProgress?.let { if (it == 1) "1 orden en proceso" else "$it órdenes en proceso" }
                    ?: "Órdenes de servicio del taller",
                showAvatar = false,
            )
        },
        fab = { OrangeFab(onClick = onNewOrder, contentDescription = "Nueva orden") },
    ) {
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.message, onRetry = vm::load)
            is UiState.Ready -> if (s.data.isEmpty) {
                MutedText(
                    "Aún no hay órdenes de servicio. Use + para registrar la recepción de un vehículo.",
                    Modifier.padding(EficazDimens.ScreenPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
                ) {
                    staffOrderSection("En proceso", s.data.inProgress, today, onOrder)
                    staffOrderSection("Recibidos", s.data.received, today, onOrder)
                    staffOrderSection("Finalizados", s.data.completed, today, onOrder)
                }
            }
        }
    }
}
