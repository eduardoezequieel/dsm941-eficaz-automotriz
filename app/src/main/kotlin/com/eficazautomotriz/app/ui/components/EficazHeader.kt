package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.eficazautomotriz.app.ui.theme.EficazColors
import com.eficazautomotriz.app.ui.theme.EficazDimens

/**
 * Navy header band drawn behind the status bar (the activity is edge-to-edge): white title,
 * light subtitle, optional back arrow at the left and optional avatar square at the right.
 *
 * When [avatarMenu] is given, tapping the avatar opens a DropdownMenu anchored to it; the
 * lambda receives `dismiss` so items can close the menu (e.g. "Cerrar sesión").
 */
@Composable
fun EficazHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarInitials: String? = null,
    onBack: (() -> Unit)? = null,
    avatarMenu: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(EficazColors.Navy)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = EficazDimens.HeaderMinHeight)
                .padding(
                    start = if (onBack != null) EficazDimens.SpaceXs else EficazDimens.HeaderPaddingStart,
                    end = EficazDimens.HeaderPaddingEnd,
                    top = EficazDimens.HeaderPaddingTop,
                    bottom = EficazDimens.HeaderPaddingBottom,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
                Spacer(Modifier.width(EficazDimens.SpaceXxs))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = EficazColors.HeaderSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            actions()
            if (avatarInitials != null) {
                Spacer(Modifier.width(EficazDimens.SpaceMd))
                HeaderAvatar(avatarInitials, avatarMenu)
            }
        }
    }
}

/** Rounded square with initials (lighter navy); optionally opens [menu] when tapped. */
@Composable
fun HeaderAvatar(
    initials: String,
    menu: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(EficazDimens.AvatarSize)
                .clip(RoundedCornerShape(EficazDimens.AvatarRadius))
                .background(EficazColors.NavyMid)
                .then(if (menu != null) Modifier.clickable { open = true } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        if (menu != null) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Color.White) {
                menu { open = false }
            }
        }
    }
}

/** Navy strip of status-bar height, for screens without a header (login), as in f1. */
@Composable
fun StatusBarBand(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(EficazColors.Navy),
    )
}

/** Up to two initials from a person or vehicle name: "Diego Escobar" → "DE", "Carlos" → "CA". */
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}
