package com.eficazautomotriz.app.ui.staff.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.EficazTheme
import com.eficazautomotriz.application.ReportRow

private const val EmptyPeriod = "Sin datos en el periodo"

/** One column of [VerticalBarChart]. A label may hold a "\n" for a two-line x label. */
data class ChartBar(val label: String, val count: Int, val color: Color)

private val YAxisWidth = 30.dp
private val ValueSpace = 22.dp
private const val BarShare = 0.64f

/**
 * Column chart of f9: y-axis ticks at the left (0, 8, 16, 24 style, see [ChartMath.axisTicks]),
 * light gridlines, bars with rounded tops, the count on top of each bar and the x labels below.
 */
@Composable
fun VerticalBarChart(bars: List<ChartBar>, modifier: Modifier = Modifier, plotHeight: Int = 170) {
    if (bars.isEmpty() || bars.all { it.count == 0 }) {
        EmptyChart(modifier)
        return
    }
    val ticks = ChartMath.axisTicks(bars.maxOf { it.count })
    val top = ticks.last()
    val measurer = rememberTextMeasurer()
    val tickStyle = MaterialTheme.typography.labelSmall.copy(color = EficazColors.TextSecondary)
    val valueStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EficazColors.TextPrimary)

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(plotHeight.dp + ValueSpace)) {
            val axisX = YAxisWidth.toPx()
            val plotTop = ValueSpace.toPx()
            val plotBottom = size.height - 1.dp.toPx()
            val plotHeightPx = plotBottom - plotTop
            val gridStroke = 1.dp.toPx()

            ticks.forEach { tick ->
                val y = plotBottom - plotHeightPx * tick / top
                drawLine(ChartColors.Grid, Offset(axisX, y), Offset(size.width, y), gridStroke)
                val label = measurer.measure("$tick", tickStyle)
                drawText(
                    label,
                    topLeft = Offset(axisX - 8.dp.toPx() - label.size.width, y - label.size.height / 2f),
                )
            }
            drawLine(ChartColors.Grid, Offset(axisX, plotTop), Offset(axisX, plotBottom), gridStroke)

            val slot = (size.width - axisX) / bars.size
            val barWidth = slot * BarShare
            val radius = 6.dp.toPx()
            bars.forEachIndexed { index, bar ->
                val left = axisX + slot * index + (slot - barWidth) / 2f
                val barHeight = plotHeightPx * ChartMath.barFraction(bar.count, top)
                if (barHeight > 0f) {
                    val r = radius.coerceAtMost(barHeight).coerceAtMost(barWidth / 2f)
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(left, plotBottom - barHeight, left + barWidth, plotBottom),
                                topLeft = CornerRadius(r, r),
                                topRight = CornerRadius(r, r),
                                bottomRight = CornerRadius.Zero,
                                bottomLeft = CornerRadius.Zero,
                            ),
                        )
                    }
                    drawPath(path, bar.color)
                }
                val value = measurer.measure("${bar.count}", valueStyle)
                drawText(
                    value,
                    topLeft = Offset(
                        left + (barWidth - value.size.width) / 2f,
                        plotBottom - barHeight - 4.dp.toPx() - value.size.height,
                    ),
                )
            }
        }
        Spacer(Modifier.height(EficazDimens.SpaceSm))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(YAxisWidth))
            bars.forEach { bar ->
                Column(
                    Modifier.weight(1f).padding(horizontal = EficazDimens.SpaceXxs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Clipped to the column so a two-line label cannot paint into its neighbor.
                    bar.label.split('\n').forEach { line ->
                        Text(
                            line,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = EficazColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Donut of f9 with the [total] and [unit] in the hole and the legend at the right: colored
 * square, name (shortened by [ChartMath.legendLabel]) and the bold whole percentage.
 */
@Composable
fun DonutChart(
    rows: List<ReportRow>,
    total: Int,
    colorFor: (ReportRow) -> Color,
    modifier: Modifier = Modifier,
    unit: String = "servicios",
) {
    if (rows.isEmpty() || rows.all { it.count == 0 }) {
        EmptyChart(modifier)
        return
    }
    val slices = rows.map { row -> row to colorFor(row) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val strokePx = 24.dp.toPx()
                val diameter = size.minDimension - strokePx
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var start = -90f
                slices.forEach { (row, color) ->
                    val sweep = ChartMath.sweepOf(row.percentage)
                    if (sweep <= 0f) return@forEach
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$total", style = MaterialTheme.typography.headlineMedium, color = EficazColors.Navy)
                Text(unit, style = MaterialTheme.typography.bodySmall, color = EficazColors.TextSecondary)
            }
        }
        Spacer(Modifier.width(EficazDimens.SectionGap))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
            slices.forEach { (row, color) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(EficazDimens.SpaceSm))
                    Text(
                        ChartMath.legendLabel(row.label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EficazColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(EficazDimens.SpaceXs))
                    Text(
                        ChartMath.wholePercent(row.percentage),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = EficazColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier = Modifier) {
    Text(
        text = EmptyPeriod,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = EficazDimens.SpaceSm),
        style = MaterialTheme.typography.bodyMedium,
        color = EficazColors.TextSecondary,
    )
}

private val previewBars = groupAppointmentRows(
    listOf(
        ReportRow("Confirmada", 21, 45.7),
        ReportRow("Atendida", 14, 30.4),
        ReportRow("Pendiente", 7, 15.2),
        ReportRow("Rechazada", 4, 8.7),
    ),
).map { (group, count) -> ChartBar(group.label, count, ChartColors.forGroup(group)) }

private val previewServices = listOf(
    ReportRow("Mantenimiento preventivo", 13, 30.0),
    ReportRow("Sistema de frenos", 10, 24.0),
    ReportRow("Diagnóstico por escáner", 8, 19.0),
    ReportRow("Sistema eléctrico", 6, 15.0),
    ReportRow("Aire acondicionado", 5, 12.0),
)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun BarChartPreview() {
    EficazTheme { VerticalBarChart(previewBars, Modifier.padding(EficazDimens.ScreenPadding)) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun DonutChartPreview() {
    EficazTheme {
        DonutChart(previewServices, total = 42, colorFor = ChartColors.byPosition(previewServices), modifier = Modifier.padding(EficazDimens.ScreenPadding))
    }
}
