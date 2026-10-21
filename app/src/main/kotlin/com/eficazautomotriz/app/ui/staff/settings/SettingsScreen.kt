package com.eficazautomotriz.app.ui.staff.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eficazautomotriz.app.ui.LocalVmFactory
import com.eficazautomotriz.app.ui.components.MessageSnackbar
import com.eficazautomotriz.app.ui.components.ReloadOnResume
import com.eficazautomotriz.app.ui.components.SegmentedTabs
import com.eficazautomotriz.app.ui.staff.SlotsScreen
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.viewmodel.staff.CatalogViewModel

private val tabs = listOf("Franjas", "Servicios", "Mantenimiento")

/** Staff configuration: weekly slots, the service catalog, and maintenance intervals. */
@Composable
fun SettingsScreen(vm: CatalogViewModel = viewModel(factory = LocalVmFactory.current)) {
    val message by vm.message.collectAsStateWithLifecycle()
    MessageSnackbar(message, vm::consumeMessage)
    ReloadOnResume(vm::load)

    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        SegmentedTabs(
            options = tabs,
            selectedIndex = selected,
            onSelect = { index ->
                if (index != selected) vm.dismissEditor()
                selected = index
            },
            modifier = Modifier.padding(start = EficazDimens.ScreenPadding, end = EficazDimens.ScreenPadding, top = EficazDimens.ScreenPadding),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                0 -> SlotsScreen()
                1 -> ServiceTypesTab(vm)
                else -> MaintenanceTypesTab(vm)
            }
        }
    }
}
