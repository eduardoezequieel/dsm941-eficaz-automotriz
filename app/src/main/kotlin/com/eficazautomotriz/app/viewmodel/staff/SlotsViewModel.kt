package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.domain.error.Outcome
import com.eficazautomotriz.domain.error.ValidationError
import com.eficazautomotriz.domain.model.TimeSlot
import com.eficazautomotriz.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

/** `effectiveCapacity` resolves capacity 0 to the system default, as the availability rule does. */
data class SlotRow(val slot: TimeSlot, val effectiveCapacity: Int)

data class SlotDay(val day: DayOfWeek, val slots: List<SlotRow>)

/** Weekly slot configuration: enable/disable and capacity per slot. */
class SlotsViewModel(graph: AppGraph, private val actor: User) : EficazViewModel(graph) {

    private val _state = MutableStateFlow<UiState<List<SlotDay>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SlotDay>>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Slot ids with a pending write: their controls stay disabled to avoid double taps. */
    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    val maxCapacity: Int get() = graph.config.maxSlotCapacity

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
                graph.appointmentService.slotCatalog()
                    .groupBy { it.dayOfWeek }
                    .toSortedMap()
                    .map { (day, slots) -> SlotDay(day, slots.map(::row)) }
            )
        }
    }

    fun setEnabled(slot: TimeSlot, enabled: Boolean) =
        write(slot.id) { graph.appointmentService.setSlotEnabled(actor, slot.id, enabled) }

    fun changeCapacity(slot: TimeSlot, delta: Int) {
        val capacity = (slot.capacity + delta).coerceIn(0, maxCapacity)
        if (capacity != slot.capacity) write(slot.id) { graph.appointmentService.setSlotCapacity(actor, slot.id, capacity) }
    }

    /** Typed capacity. The field already filters digits; the use case checks the range again. */
    fun setCapacity(slot: TimeSlot, capacity: Int) {
        ValidationError.validateSlotCapacity(capacity, maxCapacity)?.let {
            _message.value = it.message
            return
        }
        if (capacity != slot.capacity) write(slot.id) { graph.appointmentService.setSlotCapacity(actor, slot.id, capacity) }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun row(slot: TimeSlot) = SlotRow(slot, graph.appointmentService.effectiveCapacityOf(slot))

    private fun write(slotId: String, block: () -> Outcome<TimeSlot>) {
        if (slotId in _busy.value) return
        _busy.update { it + slotId }
        run({ outcome ->
            _busy.update { it - slotId }
            when (outcome) {
                is Outcome.Success -> replace(row(outcome.value))
                is Outcome.Failure -> _message.value = outcome.error.userMessage()
            }
        }, block)
    }

    private fun replace(updated: SlotRow) {
        val current = (_state.value as? UiState.Ready)?.data ?: return
        _state.value = UiState.Ready(
            current.map { day -> day.copy(slots = day.slots.map { if (it.slot.id == updated.slot.id) updated else it }) }
        )
    }
}