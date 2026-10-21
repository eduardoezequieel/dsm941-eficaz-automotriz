package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eficazautomotriz.app.ui.LocalShell
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens

/**
 * Screen frame: [header] at the top (usually [AppHeader]), [content] filling the rest on the
 * mockup background, and an optional [fab] anchored bottom-end with 16dp margins.
 */
@Composable
fun ScreenScaffold(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    fab: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(EficazColors.Background)) {
        header()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            content()
            if (fab != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(EficazDimens.SpaceLg)) { fab() }
            }
        }
    }
}

/**
 * [EficazHeader] wired to the session (via [LocalShell]): back arrow on non-tab routes and
 * the user's initials avatar, whose menu holds "Mi perfil" and "Cerrar sesión".
 * Pass `showAvatar = false` or a custom [avatarInitials] (e.g. "NF" for a vehicle) as needed.
 */
@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    showAvatar: Boolean = true,
    avatarInitials: String? = null,
) {
    val shell = LocalShell.current
    val user = shell?.user
    val initials = avatarInitials ?: user?.name?.let(::initialsOf)
    EficazHeader(
        title = title,
        subtitle = subtitle,
        onBack = shell?.onBack,
        avatarInitials = initials.takeIf { showAvatar },
        avatarMenu = if (shell != null && user != null) {
            { dismiss ->
                Text(
                    user.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "Rol: ${user.role.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EficazColors.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
                HorizontalDivider(color = EficazColors.CardBorder)
                DropdownMenuItem(text = { Text("Mi perfil") }, onClick = {
                    dismiss()
                    shell.onProfile()
                })
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", color = EficazColors.DangerText) },
                    onClick = {
                        dismiss()
                        shell.onSignOut()
                    },
                )
            }
        } else {
            null
        },
    )
}
