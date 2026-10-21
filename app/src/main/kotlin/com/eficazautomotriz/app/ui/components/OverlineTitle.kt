package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.SectionLabelStyle

/** Small section label, already uppercased by the caller so accents stay intact. */
@Composable
fun OverlineTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SectionLabelStyle,
        color = EficazColors.TextSecondary,
        modifier = modifier.padding(top = EficazDimens.SpaceMd),
    )
}
