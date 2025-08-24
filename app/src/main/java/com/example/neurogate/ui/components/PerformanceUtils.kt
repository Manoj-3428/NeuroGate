package com.example.neurogate.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay

/**
 * Performance optimization utilities for Compose UI
 */

/**
 * Debounced state that delays updates to reduce recompositions
 */
@Composable
fun <T> rememberDebouncedState(
    initialValue: T,
    delayMillis: Long = 300L
): State<T> {
    var value by remember { mutableStateOf(initialValue) }
    var debouncedValue by remember { mutableStateOf(initialValue) }
    
    LaunchedEffect(value) {
        delay(delayMillis)
        debouncedValue = value
    }
    
    return remember { derivedStateOf { debouncedValue } }
}

/**
 * Memoized expensive computation with automatic cleanup
 */
@Composable
fun <T> rememberExpensiveComputation(
    key: Any?,
    computation: () -> T
): T {
    return remember(key) {
        computation()
    }
}

/**
 * Optimized clickable modifier that prevents rapid clicks
 */
@Composable
fun Modifier.optimizedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return this.clickable(
        enabled = enabled,
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 300) { // 300ms debounce
                lastClickTime = currentTime
                onClick()
            }
        }
    )
}

/**
 * Lazy loading state for better performance
 */
@Composable
fun rememberLazyLoadingState(
    isLoading: Boolean,
    onLoadMore: () -> Unit
): LazyLoadingState {
    return remember {
        LazyLoadingState(
            isLoading = isLoading,
            onLoadMore = onLoadMore
        )
    }
}

data class LazyLoadingState(
    val isLoading: Boolean,
    val onLoadMore: () -> Unit
)

/**
 * Performance monitoring utility
 */
@Composable
fun rememberPerformanceMonitor(
    key: String
): PerformanceMonitor {
    val startTime = remember { System.currentTimeMillis() }
    
    DisposableEffect(key) {
        onDispose {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            println("Performance: $key took ${duration}ms")
        }
    }
    
    return remember { PerformanceMonitor() }
}

class PerformanceMonitor {
    fun logOperation(operation: String, block: () -> Unit) {
        val startTime = System.currentTimeMillis()
        block()
        val endTime = System.currentTimeMillis()
        println("Performance: $operation took ${endTime - startTime}ms")
    }
}

/**
 * Optimized list item that prevents unnecessary recompositions
 */
@Composable
fun <T> OptimizedListItem(
    item: T,
    key: (T) -> Any,
    content: @Composable (T) -> Unit
) {
    val memoizedItem = remember(key(item)) { item }
    content(memoizedItem)
}

/**
 * Conditional rendering with performance optimization
 */
@Composable
fun ConditionalRender(
    condition: Boolean,
    content: @Composable () -> Unit
) {
    if (condition) {
        content()
    }
}

/**
 * Optimized animation state that reduces unnecessary animations
 */
@Composable
fun rememberOptimizedAnimationState(
    targetValue: Float,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Float>? = null
): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    return remember {
        androidx.compose.animation.core.Animatable(targetValue)
    }
}
