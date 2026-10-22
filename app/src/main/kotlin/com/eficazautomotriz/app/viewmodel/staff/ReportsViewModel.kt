package com.eficazautomotriz.app.viewmodel.staff

import com.eficazautomotriz.app.di.AppGraph
import com.eficazautomotriz.app.ui.UiState
import com.eficazautomotriz.app.viewmodel.EficazViewModel
import com.eficazautomotriz.app.viewmodel.userMessage
import com.eficazautomotriz.app.viewmodel.validation.ReportPeriodValidator
import com.eficazautomotriz.application.PeriodReport
import com.eficazautomotriz.application.SystemSummary
import com.eficazautomotriz.domain.error.Outcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class Reports(
    val appointmentsByStatus: PeriodReport,
    val completedServicesByType: PeriodReport,
    val summary: SystemSummary,
)

data class ReportsState(
    val from: LocalDate,
    val to: LocalDate,
    val earliest: LocalDate,
    val latest: LocalDate,
    /** Null when the user picked the dates by hand (Personalizado). The opening range is the current month. */
    val preset: ReportPeriodPreset? = ReportPeriodPreset.MONTH,
    val periodError: String? = null,
    val reports: UiState<Reports> = UiState.Loading,
)

/** P9: both period reports (bar and donut charts) plus the live system summary. */
class ReportsViewModel(graph: AppGraph) : EficazViewModel(graph) {

    private val today: LocalDate = today()

    private val _state = MutableStateFlow(
        ReportPeriodValidator.defaultPeriod(today).let { (from, to) ->
            ReportsState(
                from = from,
                to = to,
                earliest = ReportPeriodValidator.earliestDate(today, graph.config),
                latest = ReportPeriodValidator.latestDate(today, graph.config),
            )
        }
    )
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    /** The offline demo shows mockup data; only then does the screen say the values are samples. */
    val demoMode: Boolean = graph.demoMode

    /** Only the latest load may publish: an older, slower period must not overwrite a newer one. */
    private var loadJob: Job? = null

    /** Called on resume so the summary reflects changes made in other staff screens. */
    fun load() {
        val period = _state.value
        if (period.reports !is UiState.Ready) _state.update { it.copy(reports = UiState.Loading) }
        loadJob?.cancel()
        loadJob = run({ outcome ->
            _state.update {
                it.copy(
                    reports = when (outcome) {
                        is Outcome.Success -> UiState.Ready(outcome.value)
                        is Outcome.Failure -> UiState.Error(outcome.error.userMessage())
                    },
                )
            }
        }) {
            Outcome.success(
                Reports(
                    appointmentsByStatus = graph.reportService.appointmentsByStatus(period.from, period.to),
                    completedServicesByType = graph.reportService.completedServicesByType(period.from, period.to),
                    summary = graph.reportService.systemSummary(),
                )
            )
        }
    }

    /** Week, month or quarter through today. Hand-picked dates clear the selection. */
    fun selectPreset(preset: ReportPeriodPreset) {
        val (from, to) = reportPresetRange(preset, today)
        setPeriod(from, to, preset)
    }

    fun setFrom(date: LocalDate) = setPeriod(date, _state.value.to, preset = null)

    fun setTo(date: LocalDate) = setPeriod(_state.value.from, date, preset = null)

    private fun setPeriod(from: LocalDate, to: LocalDate, preset: ReportPeriodPreset?) {
        val error = ReportPeriodValidator.error(from, to, today, graph.config)
        if (error != null) {
            _state.update { it.copy(periodError = error) }
            return
        }
        _state.update { it.copy(from = from, to = to, preset = preset, periodError = null, reports = UiState.Loading) }
        load()
    }
}
