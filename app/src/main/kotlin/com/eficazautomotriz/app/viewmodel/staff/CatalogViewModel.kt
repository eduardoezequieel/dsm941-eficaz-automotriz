package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.app.viewmodel.validation.CatalogValidator
import com.eficazautomotriz.app.viewmodel.validation.FieldErrors
import com.eficazautomotriz.app.viewmodel.validation.MileageValidator
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.model.MaintenanceType
import com.eficazautomotriz.domain.model.ServiceType
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which catalog dialog is open. The two tabs share one editor so only one dialog exists. */
enum class CatalogKind { SERVICE, MAINTENANCE }

/** Both catalogs, including inactive rows. The switches and the booking screens disagree on purpose. */
data class CatalogSnapshot(
    val services: List<ServiceType> = emptyList(),
    val maintenance: List<MaintenanceType> = emptyList(),
)

/** Fields of the add/edit dialog. [id] null means "Agregar". */
data class CatalogEditor(
    val kind: CatalogKind,
    val id: String?,
    val name: String,
    val description: String,
    val intervalKm: String,
    val active: Boolean,
    val errors: FieldErrors = FieldErrors(),
)

/** Staff catalog: service types and maintenance intervals. */
class CatalogViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<CatalogSnapshot>>(UiState.Loading)
    val state: StateFlow<UiState<CatalogSnapshot>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    private val _editor = MutableStateFlow<CatalogEditor?>(null)
    val editor: StateFlow<CatalogEditor?> = _editor.asStateFlow()

    /** Same ceiling as the odometer: an interval cannot be longer than the mileage the system accepts. */
    val intervalMaxLength: Int get() = MileageValidator.inputMaxLength(graph.config.maxMileage)

    init {
        load()
    }

    fun load() {
        run({ outcome ->
            _state.value = when (outcome) {
                is Outcome.Success -> UiState.Ready(outcome.value)
                is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
            }
        }) {
            Outcome.success(
                CatalogSnapshot(
                    services = graph.catalog.serviceTypes(),
                    maintenance = graph.catalog.maintenanceTypes(),
                ),
            )
        }
    }

    fun beginCreateService() {
        _editor.value = CatalogEditor(CatalogKind.SERVICE, id = null, name = "", description = "", intervalKm = "", active = true)
    }

    fun beginEditService(type: ServiceType) {
        _editor.value = CatalogEditor(
            CatalogKind.SERVICE,
            id = type.id,
            name = type.name,
            description = type.description,
            intervalKm = "",
            active = type.active,
        )
    }

    fun beginCreateMaintenance() {
        _editor.value = CatalogEditor(CatalogKind.MAINTENANCE, id = null, name = "", description = "", intervalKm = "", active = true)
    }

    fun beginEditMaintenance(type: MaintenanceType) {
        _editor.value = CatalogEditor(
            CatalogKind.MAINTENANCE,
            id = type.id,
            name = type.name,
            description = "",
            intervalKm = type.intervalKm.toString(),
            active = type.active,
        )
    }

    fun onNameChange(value: String) = edit { it.copy(name = value, errors = it.errors.without("name")) }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value, errors = it.errors.without("description")) }

    fun onIntervalChange(value: String) = edit { it.copy(intervalKm = value, errors = it.errors.without("intervalKm")) }

    fun onActiveChange(value: Boolean) = edit { it.copy(active = value) }

    fun dismissEditor() {
        _editor.value = null
    }

    fun save() {
        val form = _editor.value ?: return
        if (EDITOR in _busy.value) return
        when (form.kind) {
            CatalogKind.SERVICE -> {
                val (errors, input) = CatalogValidator.serviceType(form.name, form.description)
                if (input == null) {
                    _editor.value = form.copy(errors = errors)
                    return
                }
                write(EDITOR, {
                    graph.catalog.saveServiceType(actor, form.id, input.name, input.description, form.active)
                }) { saved ->
                    _editor.value = null
                    upsertService(saved)
                }
            }
            CatalogKind.MAINTENANCE -> {
                val (errors, input) = CatalogValidator.maintenanceType(form.name, form.intervalKm, graph.config)
                if (input == null) {
                    _editor.value = form.copy(errors = errors)
                    return
                }
                write(EDITOR, {
                    graph.catalog.saveMaintenanceType(actor, form.id, input.name, input.intervalKm, form.active)
                }) { saved ->
                    _editor.value = null
                    upsertMaintenance(saved)
                }
            }
        }
    }

    fun setServiceActive(type: ServiceType, active: Boolean) {
        if (type.active == active) return
        write(type.id, { graph.catalog.setServiceTypeActive(actor, type.id, active) }, ::upsertService)
    }

    fun setMaintenanceActive(type: MaintenanceType, active: Boolean) {
        if (type.active == active) return
        write(type.id, { graph.catalog.setMaintenanceTypeActive(actor, type.id, active) }, ::upsertMaintenance)
    }

    fun consumeMessage() {
        _message.value = null
    }

    /** True while the dialog save is in flight. List toggles use the row id, not this key. */
    fun isSaving(keys: Set<String>): Boolean = EDITOR in keys

    private fun edit(transform: (CatalogEditor) -> CatalogEditor) {
        _editor.update { current -> current?.let(transform) }
    }

    private fun <T> write(key: String, block: () -> Outcome<T>, onSuccess: (T) -> Unit) {
        if (key in _busy.value) return
        _busy.update { it + key }
        run({ outcome ->
            _busy.update { it - key }
            when (outcome) {
                is Outcome.Success -> onSuccess(outcome.value)
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }, block)
    }

    private fun upsertService(saved: ServiceType) {
        val current = (_state.value as? UiState.Ready)?.data ?: return load()
        _state.value = UiState.Ready(
            current.copy(services = (current.services.filter { it.id != saved.id } + saved).sortedBy { it.name }),
        )
    }

    private fun upsertMaintenance(saved: MaintenanceType) {
        val current = (_state.value as? UiState.Ready)?.data ?: return load()
        _state.value = UiState.Ready(
            current.copy(
                maintenance = (current.maintenance.filter { it.id != saved.id } + saved).sortedBy { it.name },
            ),
        )
    }

    private fun FieldErrors.without(field: String) = FieldErrors(byField - field)

    private companion object {
        const val EDITOR = "editor"
    }
}
