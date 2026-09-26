package com.hoffi.compose.complib.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Connects the sheet's drag behavior with inner scrollable content.
 *
 * Rules:
 * 1. Sheet NOT at full → all vertical scroll moves the SHEET (not content)
 * 2. Sheet at full + content NOT at top → scroll moves the CONTENT
 * 3. Sheet at full + content at top + dragging down → collapse the SHEET
 * 4. Sheet at full + content at top + dragging up → nothing (already at top)
 */
class DrawerNestedScrollConnection(
    private val drawerState: DrawerState,
    private val coroutineScope: CoroutineScope
) : NestedScrollConnection {
    // Is the inner content scrolled to the very top?
    var isContentAtTop by mutableStateOf(true)
    /**
     * Called BEFORE the child (inner LazyColumn) consumes scroll.
     * We can "steal" scroll here to move the sheet instead.
     */
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y  // Positive = finger moving down, negative = finger moving up
        // If sheet is NOT at full position → consume scroll to move the sheet
        if (drawerState.currentStop != DrawerPos.FULL) {
            coroutineScope.launch { drawerState.drag(-dy) }
            return available  // Consume all - child gets nothing
        }
        // Sheet IS at full position
        if (dy > 0 && isContentAtTop) {
            // User is dragging DOWN and content is at top
            // → Start collapsing the sheet instead of scrolling content
            coroutineScope.launch { drawerState.drag(-dy) }
            return available  // Consume all
        }
        // Otherwise, let the child handle it (normal scrolling)
        return Offset.Zero
    }
    /**
     * Called AFTER the child consumed scroll.
     * If the child couldn't consume it (e.g., reached the end), we can consume the rest.
     */
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // If there's leftover scroll after the child is done,
        // use it to move the sheet
        if (available.y != 0f && drawerState.currentStop == DrawerPos.FULL) {
            coroutineScope.launch { drawerState.drag(-available.y) }
            return available
        }
        return Offset.Zero
    }
    /**
     * Called when a fling starts BEFORE the child.
     * We intercept fast flings to move the sheet.
     */
    override suspend fun onPreFling(available: Velocity): Velocity {
        if (drawerState.currentStop != DrawerPos.FULL) {
            drawerState.settle(-available.y)
            return available  // Consume
        }
        if (available.y > 0 && isContentAtTop) {
            // Fling DOWN with content at top → settle the sheet
            drawerState.settle(-available.y)
            return available
        }
        return Velocity.Zero
    }
    /**
     * Called when a fling ends and the child didn't fully consume it.
     */
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (available.y != 0f) {
            drawerState.settle(-available.y)
            return available
        }
        return Velocity.Zero
    }
}
