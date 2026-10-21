package com.eficazautomotriz.app.ui.components

import com.eficazautomotriz.app.ui.theme.EficazDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.eficazautomotriz.app.ui.theme.ChipColors
import com.eficazautomotriz.app.ui.theme.StatusColors
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.domain.model.enums.MaintenanceStatus

/** Status pill: tinted background and colored text of the [tone] (see [StatusColors.toneForAppointment] & co.). */
@Composable
fun StatusChip(label: String, tone: StatusTone, modifier: Modifier = Modifier) {
    StatusChip(label, tone.colors, modifier)
}

@Composable
fun StatusChip(label: String, colors: ChipColors, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = colors.content,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .heightIn(min = EficazDimens.ChipHeight)
            .background(colors.background, RoundedCornerShape(50))
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = EficazDimens.ChipPaddingHorizontal, vertical = EficazDimens.SpaceXxs),
    )
}

/**
 * Legacy overload: when [color] is one of the tone text colors (what `StatusColors.for*`
 * returns) the mockup pair is used; any other color gets a faint tint of itself.
 */
@Composable
fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier) {
    val colors = StatusColors.toneOf(color)?.colors ?: ChipColors(color.copy(alpha = 0.14f), color)
    StatusChip(label, colors, modifier)
}

@Composable
fun MaintenanceBadge(status: MaintenanceStatus, modifier: Modifier = Modifier) {
    StatusChip(maintenanceShortLabel(status), StatusColors.toneForMaintenance(status), modifier)
}
