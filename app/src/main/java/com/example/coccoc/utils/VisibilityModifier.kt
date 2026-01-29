package com.example.coccoc.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun Modifier.onVisible(
    key: Any,
    onVisible: () -> Unit
): Modifier {
    val view = LocalView.current
    var hasBeenVisible by remember(key) { mutableStateOf(false) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(key, layoutCoordinates) {
        if (hasBeenVisible || layoutCoordinates == null) return@LaunchedEffect

        val coords = layoutCoordinates ?: return@LaunchedEffect
        if (!coords.isAttached) return@LaunchedEffect

        val bounds = coords.boundsInWindow()
        val insets = ViewCompat.getRootWindowInsets(view)
        val systemBarInsets = insets?.getInsets(WindowInsetsCompat.Type.systemBars())

        val screenHeight = view.height.toFloat()
        val topInset = systemBarInsets?.top?.toFloat() ?: 0f
        val bottomInset = systemBarInsets?.bottom?.toFloat() ?: 0f

        // Check if at least 50% of the item is visible in the viewport
        val visibleTop = maxOf(bounds.top, topInset)
        val visibleBottom = minOf(bounds.bottom, screenHeight - bottomInset)
        val visibleHeight = maxOf(0f, visibleBottom - visibleTop)
        val itemHeight = bounds.height

        if (itemHeight > 0 && visibleHeight / itemHeight >= 0.5f) {
            hasBeenVisible = true
            onVisible()
        }
    }

    return this.onGloballyPositioned { coordinates ->
        layoutCoordinates = coordinates
    }
}
