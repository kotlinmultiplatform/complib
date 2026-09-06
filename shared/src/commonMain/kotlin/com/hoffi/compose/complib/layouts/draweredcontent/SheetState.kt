package com.hoffi.compose.complib.layouts.draweredcontent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo

enum class SheetStop {
    HIDDEN,  // Off-screen
    PEEK,    // Just the handle + title visible (~15%)
    HALF,    // Half the screen (~50%)
    FULL     // Nearly full screen (~90%)
}
/**
 * Holds the animated offset and current logical stop of the bottom sheet.
 *
 * The offset is in pixels from the TOP of the screen.
 * - offset = screenHeight → sheet is hidden (off bottom edge)
 * - offset = screenHeight * 0.85 → peek (15% visible)
 * - offset = screenHeight * 0.50 → half (50% visible)
 * - offset = screenHeight * 0.10 → full (90% visible)
 */
class BottomSheetState(
    initialStop: SheetStop = SheetStop.HIDDEN,
    private val screenHeight: Float
) {
    // The animated Y offset (top edge of the sheet)
    val offset = Animatable(stopToOffset(initialStop))
    // Current logical stop (updated after animation completes)
    var currentStop by mutableStateOf(initialStop)
        private set
    // Convert a stop to a pixel offset from the top
    fun stopToOffset(stop: SheetStop): Float = when (stop) {
        SheetStop.HIDDEN -> screenHeight
        SheetStop.PEEK -> screenHeight * 0.85f
        SheetStop.HALF -> screenHeight * 0.50f
        SheetStop.FULL -> screenHeight * 0.10f
    }
    // All possible stop offsets (for snapping calculations)
    val stopOffsets: List<Pair<SheetStop, Float>>
        get() = SheetStop.entries
            .filter { it != SheetStop.HIDDEN }
            .map { it to stopToOffset(it) }
    // How far expanded is the sheet? 0f = peek, 1f = full
    val expandProgress: Float
        get() {
            val peekOffset = stopToOffset(SheetStop.PEEK)
            val fullOffset = stopToOffset(SheetStop.FULL)
            return ((peekOffset - offset.value) / (peekOffset - fullOffset)).coerceIn(0f, 1f)
        }
    /**
     * Snap to a specific stop with spring animation.
     * Spring physics gives a natural, physical feel with slight overshoot.
     */
    suspend fun snapTo(stop: SheetStop) {
        currentStop = stop
        offset.animateTo(
            targetValue = stopToOffset(stop),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,  // Slight bounce
                stiffness = Spring.StiffnessMedium               // Medium speed
            )
        )
    }
    /**
     * Dismiss the sheet (animate to hidden).
     */
    suspend fun dismiss() {
        currentStop = SheetStop.HIDDEN
        offset.animateTo(
            targetValue = stopToOffset(SheetStop.HIDDEN),
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }
    /**
     * After a drag gesture ends, determine which stop to snap to
     * based on current position and fling velocity.
     *
     * @param velocity pixels per second (negative = upward)
     */
    suspend fun settle(velocity: Float) {
        val currentOffset = offset.value
        val velocityThreshold = 500f  // dp/s threshold for fling detection
        val targetStop = if (kotlin.math.abs(velocity) > velocityThreshold) {
            // FAST FLING: skip to extreme stop
            if (velocity < 0) {
                // Flinging UP → go to full
                SheetStop.FULL
            } else {
                // Flinging DOWN → go to peek (or dismiss if already at peek)
                if (currentStop == SheetStop.PEEK) SheetStop.HIDDEN
                else SheetStop.PEEK
            }
        } else {
            // SLOW RELEASE: snap to nearest stop
            stopOffsets.minByOrNull { (_, offset) ->
                kotlin.math.abs(offset - currentOffset)
            }?.first ?: SheetStop.HIDDEN
        }
        if (targetStop == SheetStop.HIDDEN) {
            dismiss()
        } else {
            snapTo(targetStop)
        }
    }
    /**
     * Process a drag delta (finger movement in pixels).
     * Updates the offset immediately (no animation - follows the finger).
     */
    suspend fun drag(delta: Float) {
        val newOffset = (offset.value + delta).coerceIn(
            minimumValue = stopToOffset(SheetStop.FULL),   // Can't drag above full
            maximumValue = stopToOffset(SheetStop.HIDDEN)  // Can't drag below hidden
        )
        offset.snapTo(newOffset)
    }
}
/**
 * Remember a BottomSheetState that survives recomposition.
 */
@Composable
fun rememberBottomSheetState(
    initialStop: SheetStop = SheetStop.HIDDEN
): BottomSheetState {
    val screenHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    return remember(screenHeight) {
        BottomSheetState(initialStop = initialStop, screenHeight = screenHeight)
    }
}
