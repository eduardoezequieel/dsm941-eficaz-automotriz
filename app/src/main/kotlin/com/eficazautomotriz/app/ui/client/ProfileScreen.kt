package com.eficazautomotriz.app.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eficazautomotriz.app.ui.components.AppHeader
import com.eficazautomotriz.app.ui.components.DangerTextButton
import com.eficazautomotriz.app.ui.components.displayPhone
import com.eficazautomotriz.app.ui.components.EficazCard
import com.eficazautomotriz.app.ui.components.KeyValueRow
import com.eficazautomotriz.app.ui.components.OutlinedNavyButton
import com.eficazautomotriz.app.ui.components.ScreenScaffold
import com.eficazautomotriz.app.ui.components.SectionLabel
import com.eficazautomotriz.app.ui.components.StatusChip
import com.eficazautomotriz.app.ui.components.initialsOf
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens
import com.eficazautomotriz.app.ui.theme.StatusTone
import com.eficazautomotriz.domain.model.User

/**
 * Bottom bar "Perfil", shared by client and staff: avatar, contact data, role and
 * "Cerrar sesión". [onSettings] is only given for staff (link to Configuración).
 */
@Composable
fun ProfileScreen(
    user: User,
    onSignOut: () -> Unit,
    onSettings: (() -> Unit)? = null,
) {
    ScreenScaffold(header = { AppHeader("Mi perfil", "Rol: ${user.role.label}", showAvatar = false) }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(EficazDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceSm),
        ) {
            EficazCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(EficazColors.Navy),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initialsOf(user.name), color = Color.White, fontSize = 20.sp, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(EficazDimens.SectionGap))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EficazDimens.SpaceXs)) {
                        Text(user.name, style = MaterialTheme.typography.titleMedium, color = EficazColors.TextPrimary)
                        StatusChip(user.role.label, StatusTone.Info)
                    }
                }
            }
            SectionLabel("Datos de contacto", Modifier.padding(top = EficazDimens.SpaceSm))
            EficazCard {
                KeyValueRow("Correo electrónico", user.email.ifBlank { "Sin correo" })
                HorizontalDivider(color = EficazColors.CardBorder)
                KeyValueRow("Teléfono", displayPhone(user.phone).ifBlank { "Sin teléfono" })
                HorizontalDivider(color = EficazColors.CardBorder)
                KeyValueRow("Rol", user.role.label)
            }
            if (onSettings != null) {
                SectionLabel("Taller", Modifier.padding(top = EficazDimens.SpaceSm))
                OutlinedNavyButton("Configuración", onClick = onSettings, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.size(EficazDimens.SpaceSm))
            DangerTextButton("Cerrar sesión", onClick = onSignOut, modifier = Modifier.fillMaxWidth(), compact = false)
        }
    }
}
