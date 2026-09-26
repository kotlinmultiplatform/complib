package com.hoffi.compose.complib.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hoffi.compose.complib.EmptyComposable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DraweredContent(
    modifier: Modifier = Modifier,
    drawerDp: Dp = DrawerHandleDefaults.drawerDp,
    topDrawerState: DrawerState? = null,
    onDismissTopDrawer: () -> Unit = {},
    topDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    bottomDrawerState: DrawerState? = null,
    onDismissBottomDrawer: () -> Unit = {},
    bottomDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    leftDrawerState: DrawerState? = null,
    onDismissLeftDrawer: () -> Unit = {},
    leftDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    rightDrawerState: DrawerState? = null,
    onDismissRightDrawer: () -> Unit = {},
    rightDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    mainContent: @Composable (PaddingValues) -> Unit,
) {
    // val drawerInt = with(LocalDensity.current) { drawerDp.roundToPx() }
    val drawerInt = with(LocalDensity.current) { drawerDp.roundToPx() }
    var mainXOffset = 0.dp
    var mainYOffset = 0.dp
    var mainBottomPadding = 0.dp
    var mainEndPadding = 0.dp
    if (topDrawer !== EmptyComposable) { mainYOffset = drawerDp }
    if (bottomDrawer !== EmptyComposable) { mainBottomPadding = drawerDp }
    if (leftDrawer !== EmptyComposable) { mainXOffset = drawerDp }
    if (rightDrawer !== EmptyComposable) { mainEndPadding = drawerDp }

    val drawerOrder = remember { mutableStateListOf(0, 1, 2, 3) }
    fun bringToFront(id: Int) {
        drawerOrder.remove(id)
        drawerOrder.add(id)
    }
    fun drawerModifier(id: Int): Modifier = Modifier
        .zIndex(drawerOrder.indexOf(id).toFloat())
        .pointerInput(id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                bringToFront(id)
            }
        }

    fun updateDrawerStates(actualSize: IntSize) {
        if (topDrawerState != null) { topDrawerState.updateActualSize(actualSize) }
        if (bottomDrawerState != null) { bottomDrawerState.updateActualSize(actualSize) }
        if (leftDrawerState != null) { leftDrawerState.updateActualSize(actualSize) }
        if (rightDrawerState != null) { rightDrawerState.updateActualSize(actualSize) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().clipToBounds()) {
        val outerBoxWithConstraintsScope = this
        // mainContent
        Box(modifier = modifier.offset(x = mainXOffset, y = mainYOffset)
            .padding(bottom = mainBottomPadding, end = mainEndPadding)
            .onSizeChanged { updateDrawerStates(it) }
        ) {
            mainContent(PaddingValues())
        }

        // topDrawer
        if ( topDrawer !== EmptyComposable) {
            if (topDrawerState == null) { throw Exception("topDrawer without given topDrawerState") }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .offset { IntOffset(0, topDrawerState.animatedOffset.value.roundToInt()) }
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onSizeChanged {
                        topDrawerState.drawerSize = it
                    }
                    .then(drawerModifier(0))
            ) {
                Column {
                    Box(
                        Modifier
                            .widthIn(max = outerBoxWithConstraintsScope.maxWidth)
                            .heightIn(
                                max = (outerBoxWithConstraintsScope.maxHeight - drawerDp).coerceAtLeast(0.dp)
                            )
                    ) {
                        topDrawer(PaddingValues())
                    }
                    TopDrawerHandle(
                        modifier = Modifier
                            .pointerInput(topDrawerState) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (topDrawerState.currentStop == DrawerPos.HIDDEN) {
                                            coroutineScope.launch {
                                                topDrawerState.snapTo(DrawerPos.FULL)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                topDrawerState.dismiss()
                                            }
                                        }
                                    }
                                )
                            }
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        topDrawerState.drag(delta)
                                    }
                                },
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    coroutineScope.launch {
                                        topDrawerState.settle(velocity)
                                    }
                                }
                            ),
                        isClosed = topDrawerState.currentStop == DrawerPos.HIDDEN
                    )
                }
            }
        }

        // bottomDrawer
        if (bottomDrawer !== EmptyComposable) {
            if (bottomDrawerState == null) { throw Exception("bottomDrawer without given bottomDrawerState") }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .offset { IntOffset(0, bottomDrawerState.animatedOffset.value.roundToInt()) }
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onSizeChanged {
                        bottomDrawerState.drawerSize = it
                    }
                    .then(drawerModifier(1))
            ) {
                Column {
                    BottomDrawerHandle(
                        modifier = Modifier
                            .pointerInput(bottomDrawerState) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (bottomDrawerState.currentStop == DrawerPos.HIDDEN) {
                                            coroutineScope.launch {
                                                bottomDrawerState.snapTo(DrawerPos.FULL)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                bottomDrawerState.dismiss()
                                            }
                                        }
                                    }
                                )
                            }
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        bottomDrawerState.drag(delta)
                                    }
                                },
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    coroutineScope.launch {
                                        bottomDrawerState.settle(velocity)
                                    }
                                }
                            ),
                        isClosed = bottomDrawerState.currentStop == DrawerPos.HIDDEN
                    )
                    Box(
                        Modifier
                            .widthIn(max = outerBoxWithConstraintsScope.maxWidth)
                            .heightIn(
                                max = (outerBoxWithConstraintsScope.maxHeight - drawerDp).coerceAtLeast(0.dp)
                            )
                    ) {
                        bottomDrawer(PaddingValues())
                    }
                }
            }
        }

        // leftDrawer
        if (leftDrawer !== EmptyComposable) {
            if (leftDrawerState == null) { throw Exception("leftDrawer without given leftDrawerState") }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .offset { IntOffset(leftDrawerState.animatedOffset.value.roundToInt(), 0) }
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onSizeChanged {
                        leftDrawerState.drawerSize = it
                    }
                    .then(drawerModifier(2))
            ) {
                Row {
                    Box(
                        Modifier
                            .widthIn(
                                max = (outerBoxWithConstraintsScope.maxWidth - drawerDp).coerceAtLeast(0.dp)
                            )
                            .heightIn(max = outerBoxWithConstraintsScope.maxHeight)
                    ) {
                        leftDrawer(PaddingValues())
                    }
                    LeftDrawerHandle(
                        modifier = Modifier
                            .pointerInput(leftDrawerState) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (leftDrawerState.currentStop == DrawerPos.HIDDEN) {
                                            coroutineScope.launch {
                                                leftDrawerState.snapTo(DrawerPos.FULL)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                leftDrawerState.dismiss()
                                            }
                                        }
                                    }
                                )
                            }
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        leftDrawerState.drag(delta)
                                    }
                                },
                                orientation = Orientation.Horizontal,
                                onDragStopped = { velocity ->
                                    coroutineScope.launch {
                                        leftDrawerState.settle(velocity)
                                    }
                                }
                            ),
                        isClosed = leftDrawerState.currentStop == DrawerPos.HIDDEN
                    )
                }
            }
        }

        // rightDrawer
        if (rightDrawer !== EmptyComposable) {
            if (rightDrawerState == null) { throw Exception("rightDrawer without given rightDrawerState") }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .offset { IntOffset(rightDrawerState.animatedOffset.value.roundToInt(), 0) }
                    .fillMaxHeight()
                    .wrapContentWidth()
                    .onSizeChanged {
                        rightDrawerState.drawerSize = it
                    }
                    .then(drawerModifier(3))
            ) {
                Row {
                    RightDrawerHandle(
                        modifier = Modifier
                            .pointerInput(rightDrawerState) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (rightDrawerState.currentStop == DrawerPos.HIDDEN) {
                                            coroutineScope.launch {
                                                rightDrawerState.snapTo(DrawerPos.FULL)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                rightDrawerState.dismiss()
                                            }
                                        }
                                    }
                                )
                            }
                            .draggable(
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        rightDrawerState.drag(delta)
                                    }
                                },
                                orientation = Orientation.Horizontal,
                                onDragStopped = { velocity ->
                                    coroutineScope.launch {
                                        rightDrawerState.settle(velocity)
                                    }
                                }
                            ),
                        isClosed = rightDrawerState.currentStop == DrawerPos.HIDDEN
                    )
                    Box(
                        Modifier
                            .widthIn(
                                max = (outerBoxWithConstraintsScope.maxWidth - drawerDp).coerceAtLeast(0.dp)
                            )
                            .heightIn(max = outerBoxWithConstraintsScope.maxHeight)
                    ) {
                        rightDrawer(PaddingValues())
                    }
                }
            }
        }
    }
}
