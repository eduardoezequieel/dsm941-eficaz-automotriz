package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens

/** Shape of every mockup button (rounded rectangle). */
val ButtonShape = RoundedCornerShape(EficazDimens.ButtonRadius)

private val ButtonPadding = PaddingValues(horizontal = EficazDimens.ButtonPaddingHorizontal, vertical = EficazDimens.SpaceXs)

/**
 * Visual height: [EficazDimens.ButtonHeight] for full-width actions, [EficazDimens.ButtonHeightCompact]
 * for buttons inside cards. The touch target is kept at [EficazDimens.MinTouchTarget] by [TouchTarget].
 */
@Composable
private fun Modifier.buttonHeight(compact: Boolean): Modifier {
    val height = if (compact) EficazDimens.ButtonHeightCompact else EficazDimens.ButtonHeight
    // Material buttons never go below 40dp on their own, so the compact height is fixed;
    // with large accessibility font scales the button grows again so the label never clips.
    return if (LocalDensity.current.fontScale <= 1.3f) heightIn(min = height, max = height) else heightIn(min = height)
}

/**
 * Material pads every button to a 48dp touch target, which would add 8dp of empty layout
 * around each 40dp button; the mockup density needs 44dp (still above the 44dp guideline).
 */
@Composable
private fun TouchTarget(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides EficazDimens.MinTouchTarget, content = content)
}

@Composable
private fun ButtonLabel(text: String, busy: Boolean, color: Color) {
    if (busy) {
        CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(EficazDimens.IconSm))
    } else {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Navy filled button with white text ("Iniciar sesión", "Confirmar", "Finalizar"). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    compact: Boolean = false,
) {
    TouchTarget {
        Button(
            onClick = onClick,
            enabled = enabled && !busy,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = EficazColors.Navy,
                contentColor = Color.White,
                disabledContainerColor = EficazColors.Navy.copy(alpha = 0.45f),
                disabledContentColor = Color.White,
            ),
            modifier = modifier.buttonHeight(compact),
        ) { ButtonLabel(text, busy, Color.White) }
    }
}

/** White button with a navy border and navy text ("Ver detalle", "Reprogramar", "Continuar con Google"). */
@Composable
fun OutlinedNavyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    TouchTarget {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
            border = BorderStroke(1.dp, if (enabled) EficazColors.Navy else EficazColors.FieldBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = EficazColors.Navy,
                disabledContainerColor = Color.White,
                disabledContentColor = EficazColors.TextMuted,
            ),
            modifier = modifier.buttonHeight(compact),
        ) { ButtonLabel(text, busy = false, color = EficazColors.Navy) }
    }
}

/** Orange filled button ("Agendar ahora", "Agendar mantenimiento vencido"). */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    compact: Boolean = false,
) {
    TouchTarget {
        Button(
            onClick = onClick,
            enabled = enabled && !busy,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = EficazColors.Orange,
                contentColor = Color.White,
                disabledContainerColor = EficazColors.Orange.copy(alpha = 0.45f),
                disabledContentColor = Color.White,
            ),
            modifier = modifier.buttonHeight(compact),
        ) { ButtonLabel(text, busy, Color.White) }
    }
}

/** Red text on a light gray pill ("Cancelar cita", "Rechazar"). */
@Composable
fun DangerTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = true,
) {
    TouchTarget {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = ButtonShape,
            contentPadding = ButtonPadding,
            elevation = null,
            colors = ButtonDefaults.buttonColors(
                containerColor = EficazColors.NeutralPill,
                contentColor = EficazColors.DangerText,
                disabledContainerColor = EficazColors.NeutralPill,
                disabledContentColor = EficazColors.TextMuted,
            ),
            modifier = modifier.buttonHeight(compact),
        ) { ButtonLabel(text, busy = false, color = EficazColors.DangerText) }
    }
}

/** Orange rounded-square FAB with a white "+". */
@Composable
fun OrangeFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(EficazDimens.FabRadius),
        containerColor = EficazColors.Orange,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
        modifier = modifier.size(EficazDimens.FabSize),
    ) { Icon(Icons.Filled.Add, contentDescription = contentDescription, modifier = Modifier.size(26.dp)) }
}
