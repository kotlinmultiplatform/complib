package com.hoffi.compose.complib.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.hoffi.compose.complib.printlnErr
import kotlin.math.abs

enum class DrawerAnchor {
    TOP, BOTTOM, LEFT, RIGHT;
    override fun toString(): String = name.padStart(6)
}
enum class DrawerPos {
    HIDDEN,  // Off-screen
    PEEK,    // Just the handle + title visible (~15%)
    HALF,    // Half the screen (~50%)
    FULL;     // Nearly full screen (~90%)
    override fun toString(): String = name.padStart(6)
}
/**
 * Holds the animated offset and current logical stop of the drawer.
 *
 * The offset is in pixels from the "anchor" side of the surrounding box.
 * for topDrawer and leftDrawer, it should be negative (to offset to "outside" of the surrounding box)
 */
class DrawerState(
    val drawerAnchor: DrawerAnchor,
    initialStop: DrawerPos = DrawerPos.HIDDEN,
    val drawerInt: Int,
) {
    companion object {
        val UNINITIALIZED = IntSize(0, 0)
    }
    internal var measuredActualSize by mutableStateOf(UNINITIALIZED)
        private set
    fun updateActualSize(newSize: IntSize) {
        measuredActualSize = newSize
    }
    var drawerSize by mutableStateOf(UNINITIALIZED)
    // Current logical stop (updated after animation completes)
    var currentStop by mutableStateOf(initialStop)
        private set
    // The animated Y offset (top edge of the sheet)
    val animatedOffset = Animatable(calcTargetOffset(initialStop))
    // Convert a stop to a pixel offset from the top
    // private fun calcAndLog(drawerViz: String, f: Float): Float { printlnErr("$drawerViz: $f") ; return f }
    fun calcTargetOffset(drawerPos: DrawerPos): Float {
        val drawerExtent = when (drawerAnchor) {
            DrawerAnchor.TOP, DrawerAnchor.BOTTOM -> drawerSize.height
            DrawerAnchor.LEFT, DrawerAnchor.RIGHT -> drawerSize.width
        }
        val parentExtent = when (drawerAnchor) {
            DrawerAnchor.TOP, DrawerAnchor.BOTTOM -> measuredActualSize.height
            DrawerAnchor.LEFT, DrawerAnchor.RIGHT -> measuredActualSize.width
        }
        val visibleExtent = minOf(parentExtent, drawerExtent).toFloat()
        val visibleFraction = when (drawerPos) {
            DrawerPos.HIDDEN -> 0f
            DrawerPos.PEEK -> 0.10f
            DrawerPos.HALF -> 0.50f
            DrawerPos.FULL -> 0.90f
        }
        val exposedExtent = visibleExtent * visibleFraction

        // printlnErr("->${drawerAnchor} offsetY=${offsetY?.value}, drawerSize=$drawerSize, actualSize=$measuredActualSize  ${drawerPos} (c:${currentStop})")
        val r = when (drawerAnchor) {
            DrawerAnchor.TOP -> when (drawerPos) {
                // only the drawer handle is visible
                DrawerPos.HIDDEN -> (-drawerSize.height + drawerInt).toFloat()
                else -> exposedExtent - drawerExtent + drawerInt
            }
            DrawerAnchor.BOTTOM -> when (drawerPos) {
                DrawerPos.HIDDEN -> (measuredActualSize.height).toFloat()
                else -> parentExtent - exposedExtent
            }
            DrawerAnchor.LEFT -> when (drawerPos) {
                DrawerPos.HIDDEN -> (-drawerSize.width + drawerInt).toFloat()
                else -> exposedExtent - drawerExtent + 2 * drawerInt
            }
            DrawerAnchor.RIGHT -> when (drawerPos) {
                DrawerPos.HIDDEN -> measuredActualSize.width.toFloat()
                else -> parentExtent - exposedExtent - drawerInt
            }
        }
        printlnErr("<-${drawerAnchor} offset=${animatedOffset?.value}, drawerSize=$drawerSize, actualSize=$measuredActualSize  $drawerPos (c:${currentStop}) -> $r ")
        return r
    }
    // All possible stop offsets (for snapping calculations)
    val allDrawerOffsets: List<Pair<DrawerPos, Float>>
        get() = DrawerPos.entries.map { it to calcTargetOffset(it) }
    fun nearestDrawerStop(): DrawerPos {
        allDrawerOffsets.forEach { printlnErr("all: offset=${animatedOffset.value}, ${it.second}:${it.first}") }

        val nearestDrawerPos = allDrawerOffsets
            .minByOrNull { (_, offset) -> abs(offset - animatedOffset.value) }
            ?.first ?: DrawerPos.HIDDEN
        printlnErr("nearestDrawerStop: offset=${animatedOffset.value}, nearestDrawerPos=$nearestDrawerPos")
        return nearestDrawerPos
    }
    // How far expanded is the sheet? 0f = peek, 1f = full
    val expandProgress: Float
        get() {
            val peekOffset = calcTargetOffset(DrawerPos.PEEK)
            val fullOffset = calcTargetOffset(DrawerPos.FULL)
            return ((peekOffset - animatedOffset.value) / (peekOffset - fullOffset)).coerceIn(0f, 1f) // TODO hoffi
        }
    /**
     * Snap to a specific stop with spring animation.
     * Spring physics gives a natural, physical feel with slight overshoot.
     */
    suspend fun snapTo(stop: DrawerPos) {
        currentStop = stop
        animatedOffset.animateTo(
            targetValue = calcTargetOffset(stop),
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
        currentStop = DrawerPos.HIDDEN
        animatedOffset.animateTo(
            targetValue = calcTargetOffset(DrawerPos.HIDDEN),
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
        val velocityThreshold = 900f  // dp/s threshold for fling detection
        val targetStop = if (abs(velocity) > velocityThreshold) {
            // FAST FLING: skip to extreme stop
            when (drawerAnchor) {
                DrawerAnchor.TOP -> if (velocity < 0) DrawerPos.HIDDEN else { DrawerPos.FULL }
                DrawerAnchor.BOTTOM -> if (velocity < 0) DrawerPos.FULL else { DrawerPos.HIDDEN }
                DrawerAnchor.LEFT -> if (velocity < 0) DrawerPos.HIDDEN else { DrawerPos.FULL }
                DrawerAnchor.RIGHT -> if (velocity < 0) DrawerPos.FULL else { DrawerPos.HIDDEN }
            }
        } else {
            // SLOW RELEASE: snap to nearest stop
            nearestDrawerStop()
        }
        printlnErr("settle: velocity=$velocity, targetStop=$targetStop, currentStop=$currentStop")
        when (targetStop) {
            DrawerPos.HIDDEN -> { dismiss() }
            DrawerPos.PEEK if drawerAnchor == DrawerAnchor.TOP && velocity < 0 -> dismiss()
            DrawerPos.PEEK if drawerAnchor == DrawerAnchor.BOTTOM && velocity > 0 -> dismiss()
            DrawerPos.PEEK if drawerAnchor == DrawerAnchor.LEFT && velocity < 0 -> dismiss()
            DrawerPos.PEEK if drawerAnchor == DrawerAnchor.RIGHT && velocity > 0 -> dismiss()
            else -> snapTo(targetStop)
        }
    }

    /**
     * Process a drag delta (finger movement in pixels).
     * Updates the offset immediately (no animation - follows the finger).
     */
    suspend fun drag(delta: Float) {
        printlnErr("drag: delta=$delta, offset=${animatedOffset.value}, currentStop=$currentStop")
        val newOffset = when (drawerAnchor) {
            DrawerAnchor.TOP -> {
                (animatedOffset.value + delta).coerceIn(
                    minimumValue = calcTargetOffset(DrawerPos.HIDDEN),   // Can't drag above HIDDEN
                    maximumValue = calcTargetOffset(DrawerPos.FULL)  // Can't drag below FULL
                )
            }
            DrawerAnchor.BOTTOM -> {
                (animatedOffset.value + delta).coerceIn(
                    minimumValue = calcTargetOffset(DrawerPos.FULL),   // Can't drag above full
                    maximumValue = calcTargetOffset(DrawerPos.HIDDEN)  // Can't drag below hidden
                )
            }
            DrawerAnchor.LEFT -> {
                (animatedOffset.value + delta).coerceIn(
                    minimumValue = calcTargetOffset(DrawerPos.HIDDEN),
                    maximumValue = calcTargetOffset(DrawerPos.FULL)
                )
            }
            DrawerAnchor.RIGHT -> {
                (animatedOffset.value + delta).coerceIn(
                    minimumValue = calcTargetOffset(DrawerPos.FULL),
                    maximumValue = calcTargetOffset(DrawerPos.HIDDEN)
                )
            }
        }
        animatedOffset.snapTo(newOffset)
    }

    suspend fun refreshOffsetForCurrentStop() {
        if (measuredActualSize == UNINITIALIZED || drawerSize == UNINITIALIZED) return
        animatedOffset.snapTo(calcTargetOffset(currentStop))
    }
}

@Composable
fun rememberDrawerState(
    drawerAnchor: DrawerAnchor,
    drawerInt: Int,
    initialStop: DrawerPos = DrawerPos.HIDDEN
): DrawerState {
    val drawerState = remember(drawerAnchor, drawerInt, initialStop) {
        DrawerState(drawerAnchor, initialStop, drawerInt)
    }
    LaunchedEffect(drawerState, drawerState.measuredActualSize, drawerState.drawerSize) {
        drawerState.refreshOffsetForCurrentStop()
    }
    return drawerState
}
