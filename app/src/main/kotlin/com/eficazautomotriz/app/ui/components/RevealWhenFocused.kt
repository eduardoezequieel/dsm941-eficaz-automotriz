package com.eficazautomotriz.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Scrolls the whole field into view once the keyboard is up, plus [below] of room for its
 * error line, counter or the button under it. A focused text field only reveals its cursor
 * line, which left tall fields (reception notes) half hidden behind the keyboard.
 * It needs the root `imePadding()` in AppRoot so the scroll container shrinks first.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
fun Modifier.revealWhenFocused(below: Dp = 72.dp): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val imeVisible = WindowInsets.isImeVisible
    val extra = with(LocalDensity.current) { below.toPx() }
    LaunchedEffect(focused, imeVisible, size) {
        if (focused && imeVisible && size != IntSize.Zero) {
            requester.bringIntoView(Rect(0f, 0f, size.width.toFloat(), size.height + extra))
        }
    }
    bringIntoViewRequester(requester)
        .onSizeChanged { size = it }
        .onFocusChanged { focused = it.hasFocus }
}
