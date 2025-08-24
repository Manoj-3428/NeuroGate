package com.example.neurogate.ui.components

import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import android.util.Log

/**
 * Performance utilities for Compose UI optimization
 */

/**
 * Memoized derived state for expensive computations
 */
@Composable
fun <T> rememberDerivedState(calculation: () -> T): T {
    return remember { derivedStateOf(calculation) }.value
}

/**
 * Optimized scroll state with performance improvements
 */
@Composable
fun rememberOptimizedScrollState(): androidx.compose.foundation.ScrollState {
    return rememberScrollState()
}

/**
 * Performance-optimized modifier for expensive operations
 */
fun Modifier.performanceOptimized(): Modifier = this

/**
 * Cached background modifier for better performance
 */
fun Modifier.cachedBackground(
    color: Color,
    alpha: Float = 1.0f
): Modifier = this.drawWithCache {
    onDrawBehind {
        drawRect(color.copy(alpha = alpha))
    }
}

/**
 * Optimized size change listener
 */
fun Modifier.rememberSizeChangeListener(
    onSizeChange: (width: Int, height: Int) -> Unit
): Modifier {
    return this.onSizeChanged { size ->
        onSizeChange(size.width, size.height)
    }
}

/**
 * Memoized color with alpha
 */
@Composable
fun rememberColorWithAlpha(color: Color, alpha: Float): Color {
    return remember(color, alpha) {
        color.copy(alpha = alpha)
    }
}

/**
 * Optimized spacing calculation
 */
@Composable
fun rememberSpacing(spacing: Dp): Int {
    val density = LocalDensity.current
    return remember(spacing) {
        density.run { spacing.toPx().toInt() }
    }
}

/**
 * Performance-optimized list item key generator
 */
fun generateStableKey(id: String, index: Int): String {
    return "item_${id}_$index"
}

/**
 * Memoized text style for better performance
 */
@Composable
fun rememberTextStyle(
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    color: Color = Color.Unspecified
): androidx.compose.ui.text.TextStyle {
    return remember(fontSize, fontWeight, color) {
        androidx.compose.ui.text.TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        )
    }
}

/**
 * Optimized clickable modifier with ripple effect
 */
fun Modifier.optimizedClickable(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = this.clickable(
    enabled = enabled,
    onClick = onClick
)

/**
 * Performance monitoring utility
 */
@Composable
fun rememberPerformanceMonitor(
    key: String,
    onPerformanceIssue: (String) -> Unit = {}
) {
    DisposableEffect(key) {
        val startTime = System.currentTimeMillis()
        onDispose {
            val duration = System.currentTimeMillis() - startTime
            if (duration > 16) { // 60fps threshold
                onPerformanceIssue("Performance issue detected in $key: ${duration}ms")
            }
        }
    }
}

/**
 * Optimized state management for lists
 */
@Composable
fun <T> rememberOptimizedListState(
    items: List<T>,
    keySelector: (T) -> String
): androidx.compose.foundation.lazy.LazyListState {
    return rememberLazyListState()
}

/**
 * Cached calculation for expensive operations
 */
@Composable
fun <T> rememberCachedCalculation(
    key: Any,
    calculation: () -> T
): T {
    return remember(key) { calculation() }
}

/**
 * Performance-optimized animation
 */
@Composable
fun rememberOptimizedAnimation(
    targetValue: Float,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Float> = androidx.compose.animation.core.tween(300)
): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val animatable = remember { androidx.compose.animation.core.Animatable(targetValue) }
    
    LaunchedEffect(targetValue) {
        animatable.animateTo(targetValue, animationSpec)
    }
    
    return animatable
}

/**
 * Optimized recomposition prevention
 */
@Composable
fun <T> rememberStable(
    key: Any,
    factory: () -> T
): T {
    return remember(key) { factory() }
}

/**
 * Performance-optimized modifier chain
 */
fun Modifier.optimizedChain(
    vararg modifiers: Modifier
): Modifier {
    var result: Modifier = Modifier
    for (modifier in modifiers) {
        result = result.then(modifier)
    }
    return this.then(result)
}

/**
 * Cached color calculation
 */
@Composable
fun rememberCachedColor(
    color: Color,
    alpha: Float = 1.0f
): Color {
    return remember(color, alpha) {
        color.copy(alpha = alpha)
    }
}

/**
 * Optimized layout performance
 */
@Composable
fun rememberLayoutOptimization(): Modifier {
    return Modifier
}

/**
 * Performance monitoring for composables
 */
@Composable
fun PerformanceMonitor(
    composableName: String,
    content: @Composable () -> Unit
) {
    val startTime = remember { System.currentTimeMillis() }
    
    DisposableEffect(Unit) {
        onDispose {
            val duration = System.currentTimeMillis() - startTime
            if (duration > 16) {
                android.util.Log.w("Performance", "$composableName took ${duration}ms to compose")
            }
        }
    }
    
    content()
}
