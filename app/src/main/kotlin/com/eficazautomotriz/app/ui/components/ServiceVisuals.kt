package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eficazautomotriz.app.ui.format.Formats
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.ServiceOrder
import com.eficazautomotriz.domain.model.enums.EvidenceStage
import com.eficazautomotriz.domain.model.enums.ServiceStatus
import java.time.LocalDateTime

/**
 * Three evidence tiles, one per stage (f2 "ÚLTIMO SERVICIO", f7 "EVIDENCIAS FOTOGRÁFICAS").
 * The labeled [PlaceholderThumb] is always drawn and the photo, when there is one, on top of
 * it: while loading or when the URL fails the tile still looks like the mockup. [labelOf]
 * lets f2 call the middle stage "Proceso" while f7 says "Diagnóstico".
 */
@Composable
fun EvidencePhotoTiles(
    evidences: List<Evidence>,
    modifier: Modifier = Modifier,
    tileHeight: Dp = EficazDimens.ThumbHeight,
    labelOf: (EvidenceStage) -> String = ::evidenceStageLabel,
) {
    val latestByStage = evidences.groupBy { it.stage }.mapValues { (_, items) -> items.maxBy { it.uploadedAt } }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        EvidenceStage.entries.forEach { stage ->
            val label = labelOf(stage)
            Box(
                Modifier
                    .weight(1f)
                    .height(tileHeight)
                    .clip(RoundedCornerShape(EficazDimens.ThumbRadius)),
            ) {
                PlaceholderThumb(label, Modifier.fillMaxSize())
                latestByStage[stage]?.imageUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * f7 "SEGUIMIENTO DE ESTADOS": white card with Recibido → En proceso → Finalizado. Reached
 * steps get a filled green dot and a green connector; pending ones a gray ring.
 */
@Composable
fun ServiceTimelineCard(
    order: ServiceOrder,
    modifier: Modifier = Modifier,
    timeText: (LocalDateTime) -> String = Formats::dateTime,
) {
    val steps = listOf(
        Triple("Recibido", true, timeText(order.receivedAt)),
        Triple("En proceso", order.status != ServiceStatus.RECEIVED, order.startedAt?.let(timeText)),
        Triple(
            "Finalizado",
            order.status == ServiceStatus.COMPLETED,
            order.closedAt?.let { "${timeText(it)} · agregado al historial" },
        ),
    )
    EficazCard(modifier) {
        steps.forEachIndexed { index, (title, reached, detail) ->
            Row {
                Column(Modifier.width(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(top = EficazDimens.SpaceXxs)
                            .size(16.dp)
                            .clip(CircleShape)
                            .then(
                                if (reached) Modifier.background(EficazColors.SuccessText)
                                else Modifier.border(2.dp, EficazColors.FieldBorder, CircleShape),
                            ),
                    )
                    if (index < steps.lastIndex) {
                        val nextReached = steps[index + 1].second
                        Box(
                            Modifier
                                .padding(vertical = EficazDimens.SpaceXxs)
                                .size(width = 2.dp, height = 18.dp)
                                .background(if (nextReached) EficazColors.SuccessText else EficazColors.CardBorder),
                        )
                    }
                }
                Column(Modifier.padding(start = EficazDimens.SpaceMd)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (reached) EficazColors.TextPrimary else EficazColors.TextMuted,
                    )
                    Text(
                        detail ?: "Pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = EficazColors.TextSecondary,
                    )
                }
            }
        }
    }
}
