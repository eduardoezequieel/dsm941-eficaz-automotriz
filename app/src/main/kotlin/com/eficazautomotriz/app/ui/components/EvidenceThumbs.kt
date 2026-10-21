package com.eficazautomotriz.app.ui.components

import com.eficazautomotriz.app.ui.theme.EficazDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eficazautomotriz.domain.model.Evidence
import com.eficazautomotriz.domain.model.enums.EvidenceStage

/** One thumbnail per stage. A missing photo keeps the labeled placeholder box. */
@Composable
fun EvidenceStageThumbnails(evidences: List<Evidence>, modifier: Modifier = Modifier) {
    val latestByStage = evidences.groupBy { it.stage }.mapValues { (_, items) -> items.maxBy { it.uploadedAt } }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm)) {
        EvidenceStage.entries.forEach { stage ->
            EvidenceThumb(
                url = latestByStage[stage]?.imageUrl,
                label = evidenceStageLabel(stage),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EvidenceThumb(url: String?, label: String, modifier: Modifier) {
    val placeholder = MaterialTheme.colorScheme.primaryContainer
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(EficazDimens.ThumbRadius))
                .background(placeholder),
            contentAlignment = Alignment.Center,
        ) {
            if (url == null) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(placeholder),
                    error = ColorPainter(placeholder),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
