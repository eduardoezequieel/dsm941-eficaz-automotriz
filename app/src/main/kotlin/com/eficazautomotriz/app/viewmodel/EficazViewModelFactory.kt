package com.eficazautomotriz.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.staff.CatalogViewModel
import com.eficazautomotriz.app.viewmodel.staff.CompleteOrderViewModel
import com.eficazautomotriz.app.viewmodel.staff.EvidenceViewModel
import com.eficazautomotriz.app.viewmodel.staff.NewOrderViewModel
import com.eficazautomotriz.app.viewmodel.staff.OrderDetailViewModel
import com.eficazautomotriz.app.viewmodel.staff.ReportsViewModel
import com.eficazautomotriz.app.viewmodel.staff.SlotsViewModel
import com.eficazautomotriz.app.viewmodel.staff.StaffAppointmentViewModel
import com.eficazautomotriz.app.viewmodel.staff.StaffPanelViewModel
import com.eficazautomotriz.app.viewmodel.staff.StaffServicesViewModel
import com.eficazautomotriz.domain.model.User

/**
 * Single ViewModel factory (no Hilt or Koin). One instance per session: `actor` is the
 * signed-in user passed to the use cases; route arguments arrive through SavedStateHandle.
 */
class EficazViewModelFactory(
    private val graph: AppGraph,
    private val actor: User?,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel: ViewModel = when (modelClass) {
            SessionViewModel::class.java -> SessionViewModel(graph)
            AuthViewModel::class.java -> AuthViewModel(graph)
            ClientHomeViewModel::class.java -> ClientHomeViewModel(graph, requireActor())
            VehiclesViewModel::class.java -> VehiclesViewModel(graph, requireActor())
            VehicleFormViewModel::class.java -> VehicleFormViewModel(graph, requireActor())
            VehicleDetailViewModel::class.java -> VehicleDetailViewModel(graph, requireActor(), routeId(extras))
            MileageViewModel::class.java -> MileageViewModel(graph, requireActor(), routeId(extras))
            AppointmentsViewModel::class.java -> AppointmentsViewModel(graph, requireActor())
            AppointmentDetailViewModel::class.java -> AppointmentDetailViewModel(graph, requireActor(), routeId(extras))
            NewAppointmentViewModel::class.java -> NewAppointmentViewModel(graph, requireActor())
            ServiceHistoryViewModel::class.java -> ServiceHistoryViewModel(graph, requireActor(), routeId(extras))
            ClientHistoryViewModel::class.java -> ClientHistoryViewModel(graph, requireActor())
            ServiceDetailViewModel::class.java -> ServiceDetailViewModel(graph, requireActor(), routeId(extras))
            StaffPanelViewModel::class.java -> StaffPanelViewModel(graph, requireActor())
            StaffServicesViewModel::class.java -> StaffServicesViewModel(graph, requireActor())
            StaffAppointmentViewModel::class.java -> StaffAppointmentViewModel(graph, requireActor(), routeId(extras))
            NewOrderViewModel::class.java ->
                NewOrderViewModel(graph, requireActor(), extras.createSavedStateHandle().get<String>(APPOINTMENT_ID))
            OrderDetailViewModel::class.java -> OrderDetailViewModel(graph, requireActor(), routeId(extras))
            EvidenceViewModel::class.java -> EvidenceViewModel(graph, requireActor(), routeId(extras))
            CompleteOrderViewModel::class.java -> CompleteOrderViewModel(graph, requireActor(), routeId(extras))
            ReportsViewModel::class.java -> ReportsViewModel(graph)
            SlotsViewModel::class.java -> SlotsViewModel(graph, requireActor())
            CatalogViewModel::class.java -> CatalogViewModel(graph, requireActor())
            else -> throw IllegalArgumentException("Unregistered ViewModel: ${modelClass.name}")
        }
        return modelClass.cast(viewModel)!!
    }

    private fun requireActor(): User = checkNotNull(actor) { "Session screen opened without a signed-in user" }

    private fun routeId(extras: CreationExtras): String =
        checkNotNull(extras.createSavedStateHandle().get<String>(ROUTE_ID)) { "Route without the {$ROUTE_ID} argument" }

    private companion object {
        const val ROUTE_ID = "id"
        const val APPOINTMENT_ID = "appointmentId"
    }
}
