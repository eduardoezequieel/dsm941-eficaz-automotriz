package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.app.viewmodel.validation.MileageValidator
import com.eficazautomotriz.domain.error.DomainError
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.User
import com.eficazautomotriz.domain.model.Vehicle
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CompleteOrderState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val order: ServiceOrder? = null,
    val vehicle: Vehicle? = null,
    val maintenanceTypes: List<MaintenanceType> = emptyList(),
    val selectedTypeIds: Set<String> = emptySet(),
    val mileage: String = "",
    val mileageError: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val completed: Boolean = false,
)

/**
 * Closes an order: final mileage (never below the vehicle's) and the maintenance jobs that
 * were performed, which restart their cycle through `ServiceOrderService.complete`.
 */
class CompleteOrderViewModel(
    graph: AppGraph,
    private val actor: User,
    private val orderId: String,
) : EficazViewModel(graph) {

    private val _state = MutableStateFlow(CompleteOrderState())
    val state: StateFlow<CompleteOrderState> = _state.asStateFlow()

    /** Digits of the odometer ceiling, plus the thousands commas the field accepts. */
    val mileageMaxLength: Int get() = MileageValidator.inputMaxLength(graph.config.maxMileage)

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> outcome.value
                    is Outcome.Failure -> it.copy(loading = false, loadError = outcome.error.userMessage())
                }
            }
        }) {
            val order = when (val detail = graph.serviceOrderService.detailOf(actor, orderId)) {
                is Outcome.Success -> detail.value.order
                is Outcome.Failure -> return@run detail
            }
            // Reached only from an in-progress order, but a stale back stack could land here.
            if (OrderAction.COMPLETE !in orderActionsFor(order.status)) {
                return@run Outcome.failure(DomainError.InvalidTransition(order.status, ServiceStatus.COMPLETED))
            }
            val vehicle = when (val found = graph.vehicleService.findAccessible(actor, order.vehicleId)) {
                is Outcome.Success -> found.value
                is Outcome.Failure -> return@run found
            }
            Outcome.success(
                CompleteOrderState(
                    loading = false,
                    order = order,
                    vehicle = vehicle,
                    maintenanceTypes = graph.maintenanceService.activeTypes(),
                    mileage = vehicle.currentMileage.toString(),
                )
            )
        }
    }

    fun onMileage(value: String) = _state.update { it.copy(mileage = value, mileageError = null) }

    fun toggleType(typeId: String) = _state.update {
        it.copy(selectedTypeIds = if (typeId in it.selectedTypeIds) it.selectedTypeIds - typeId else it.selectedTypeIds + typeId)
    }

    fun submit() {
        val form = _state.value
        val vehicle = form.vehicle ?: return
        if (form.saving) return
        val (error, mileage) = MileageValidator.validate(form.mileage, vehicle.currentMileage, graph.config)
        if (mileage == null) {
            _state.update { it.copy(mileageError = error) }
            return
        }
        _state.update { it.copy(saving = true, message = null) }
        // Keep the catalog order so the baselines are written deterministically.
        val performed = form.maintenanceTypes.map { it.id }.filter { it in form.selectedTypeIds }
        run({ outcome ->
            _state.update {
                when (outcome) {
                    is Outcome.Success -> it.copy(saving = false, completed = true)
                    is Outcome.Failure -> it.copy(saving = false, message = outcome.error.userMessage())
                }
            }
        }) { graph.serviceOrderService.complete(actor, orderId, mileage, performed, today(), now()) }
    }
}
