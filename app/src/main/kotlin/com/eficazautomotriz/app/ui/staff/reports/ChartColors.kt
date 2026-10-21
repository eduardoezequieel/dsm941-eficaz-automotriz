package com.eficazautomotriz.app.ui.staff.reports

import androidx.compose.ui.graphics.Color
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.application.ReportRow

/**
 * Series palette of the f9 charts, sampled from the mockup. It is deliberately not the chip
 * palette ([com.eficazautomotriz.app.ui.theme.StatusColors]): chart series need strong,
 * distinct fills, chips need pale tints.
 */
object ChartColors {
    /** Second bar and second slice of f9. */
    val MidBlue = Color(0xFF1D5C7E)

    /** Fourth slice of f9 ("Sistema eléctrico"). */
    val SteelBlue = Color(0xFF7FA6BB)

    /** Fifth slice of f9 ("Aire acondicionado"). */
    val PaleSlate = Color(0xFFC4D2DA)

    /** Gridlines and axis lines behind the bars. */
    val Grid = EficazColors.CardBorder

    private val slices: List<Color> = listOf(
        EficazColors.Navy,
        MidBlue,
        EficazColors.Orange,
        SteelBlue,
        PaleSlate,
        Color(0xFF4F8FB0),
        Color(0xFFF2B27A),
        EficazColors.TextMuted,
    )

    /** Bar color of each appointment group, in the f9 order: navy, mid blue, orange, red. */
    fun forGroup(group: AppointmentBarGroup): Color = when (group) {
        AppointmentBarGroup.CONFIRMED -> EficazColors.Navy
        AppointmentBarGroup.ATTENDED -> MidBlue
        AppointmentBarGroup.OPEN -> EficazColors.Orange
        AppointmentBarGroup.CLOSED -> EficazColors.DangerText
    }

    /** Donut colors by position within the report, so two types never share a color. */
    fun byPosition(rows: List<ReportRow>): (ReportRow) -> Color {
        val colors = rows.mapIndexed { index, row -> row.label to slices[index % slices.size] }.toMap()
        return { row -> colors[row.label] ?: slices.first() }
    }
}
