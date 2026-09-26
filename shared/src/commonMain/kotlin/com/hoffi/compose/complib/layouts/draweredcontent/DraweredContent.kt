package com.hoffi.compose.complib.layouts.draweredcontent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hoffi.compose.complib.debugBorder
import com.hoffi.compose.complib.EmptyComposable
import com.hoffi.compose.complib.components.BottomDrawerHandle
import com.hoffi.compose.complib.components.DrawerHandleDefaults
import com.hoffi.compose.complib.components.LeftDrawerHandle
import com.hoffi.compose.complib.components.RightDrawerHandle
import com.hoffi.compose.complib.components.TopDrawerHandle

class DraweredContentInfo() {
    var mainPlaceables: List<Placeable> = emptyList()

    var topDrawerPlacables: List<Placeable> = emptyList()
    var bottomDrawerPlacables: List<Placeable> = emptyList()
    var leftDrawerPlacables: List<Placeable> = emptyList()
    var rightDrawerPlacables: List<Placeable> = emptyList()

    var topDrawerSize: IntSize = IntSize.Zero
    var bottomDrawerSize: IntSize = IntSize.Zero
    var leftDrawerSize: IntSize = IntSize.Zero
    var rightDrawerSize: IntSize = IntSize.Zero

    var mainSize: IntSize = IntSize.Zero
    var mainHeightConstraint: Dp = 0.dp
    var mainWidthConstraint: Dp = 0.dp
}

@Composable
fun DraweredContent(
    modifier: Modifier = Modifier,
    drawerDp: Dp = DrawerHandleDefaults.drawerDp,
    topDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    bottomDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    leftDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    rightDrawer: @Composable (PaddingValues) -> Unit = EmptyComposable,
    mainContent: @Composable (PaddingValues) -> Unit,
) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier) {
            DraweredContentLayout(
                drawerDp = drawerDp,
                mainContent = mainContent,
                topDrawer = topDrawer,
                bottomDrawer = bottomDrawer,
                leftDrawer = leftDrawer,
                rightDrawer = rightDrawer,
            )
        }
    }
    child(modifier)
}

@Composable
private fun DraweredContentLayout(
    drawerDp: Dp,
    mainContent: @Composable (PaddingValues) -> Unit,
    topDrawer: @Composable (PaddingValues) -> Unit,
    bottomDrawer: @Composable (PaddingValues) -> Unit,
    leftDrawer: @Composable (PaddingValues) -> Unit,
    rightDrawer: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val givenMaxWidth = constraints.maxWidth
        val givenMaxHeight = constraints.maxHeight

        val ci = DraweredContentInfo()


        if (topDrawer !== EmptyComposable) {
            ci.mainHeightConstraint += drawerDp
            // ==============================+
            // measure (by composing) top drawerHandle and get its size
            // ==============================+
            ci.topDrawerPlacables = subcompose(DraweredContentLayoutSlots.TopDrawer) {
                Column(Modifier.wrapContentHeight().debugBorder()) {
                    Box(
                        Modifier.heightIn(
                            max = with(LocalDensity.current) {
                                (constraints.maxHeight - drawerDp.roundToPx()).coerceAtLeast(0).toDp()
                            }
                        )
                    ) {
                        topDrawer(PaddingValues())
                    }
                    TopDrawerHandle(handleHeight = drawerDp)
                }
            }.map { it.measure(constraints) }
            ci.topDrawerSize = IntSize(
                width = ci.topDrawerPlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.topDrawerPlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (bottomDrawer !== EmptyComposable) {
            ci.mainHeightConstraint += drawerDp
            // ==============================+
            // measure (by composing) bottom drawerHandle and get its size
            // ==============================+
            ci.bottomDrawerPlacables = subcompose(DraweredContentLayoutSlots.BottomDrawer) {
                Column(Modifier.fillMaxWidth().debugBorder()) {
                    BottomDrawerHandle(handleHeight = drawerDp)
                    bottomDrawer(PaddingValues())
                }
            }.map { it.measure(constraints) }
            ci.bottomDrawerSize = IntSize(
                width = ci.bottomDrawerPlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.bottomDrawerPlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (leftDrawer !== EmptyComposable) {
            ci.mainWidthConstraint += drawerDp
            // ==============================+
            // measure (by composing) left drawerHandle and get its size
            // ==============================+
            ci.leftDrawerPlacables = subcompose(DraweredContentLayoutSlots.LeftDrawer) {
                Row(Modifier.fillMaxHeight().debugBorder()) {
                    leftDrawer(PaddingValues())
                    LeftDrawerHandle(handleWidth = drawerDp)
                }
            }.map { it.measure(constraints) }
            ci.leftDrawerSize = IntSize(
                width = ci.leftDrawerPlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.leftDrawerPlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (rightDrawer !== EmptyComposable) {
            ci.mainWidthConstraint += drawerDp
            // ==============================+
            // measure (by composing) right drawerHandle and get its size
            // ==============================+
            ci.rightDrawerPlacables = subcompose(DraweredContentLayoutSlots.RightDrawer) {
                Row(Modifier.fillMaxHeight().debugBorder()) {
                    RightDrawerHandle(handleWidth = drawerDp)
                    rightDrawer(PaddingValues())
                }
            }.map { it.measure(constraints) }
            ci.rightDrawerSize = IntSize(
                width = ci.rightDrawerPlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.rightDrawerPlacables.maxOfOrNull { it.height } ?: 0
            )
        }

        val mainContentMaxHeight = (givenMaxHeight - ci.mainHeightConstraint.roundToPx()).coerceAtLeast(0)
        val mainContentMaxWidth = (givenMaxWidth - ci.mainWidthConstraint.roundToPx()).coerceAtLeast(0)

        val mainHeightPadding = if (bottomDrawer !== EmptyComposable) {drawerDp} else {0.dp}
        val mainWidthPadding = if (rightDrawer !== EmptyComposable) {drawerDp} else {0.dp}
        val mainPadding = PaddingValues(bottom = mainHeightPadding, end = mainWidthPadding)
        val mainConstraints = constraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

        // ====================================+
        // measure mainContent
        // ====================================+
        ci.mainPlaceables = subcompose(DraweredContentLayoutSlots.MainContent) {
            mainContent(mainPadding)
        }.map { it.measure(mainConstraints) }
        ci.mainSize = IntSize(
            width = ci.mainPlaceables.maxOfOrNull { it.width } ?: 0,
            height = ci.mainPlaceables.maxOfOrNull { it.height } ?: 0
        )


        val eventualLayoutWidth = ci.leftDrawerSize.width + ci.mainSize.width + ci.rightDrawerSize.width
        val eventualLayoutHeight = ci.topDrawerSize.height + ci.mainSize.height + ci.bottomDrawerSize.height

        // ==========================================================================================================+
        // and place the subcomposed (pre rendered and padded) Placeables into the layout
        // ==========================================================================================================+
        layout(eventualLayoutWidth, eventualLayoutHeight) {
            // The bottom bar is always at the bottom of the layout
            ci.bottomDrawerPlacables.forEach {
                it.place(0, eventualLayoutHeight - ci.bottomDrawerSize.height)
            }
            ci.rightDrawerPlacables.forEach {
                it.place( eventualLayoutWidth - ci.rightDrawerSize.width, 0)
            }
            ci.leftDrawerPlacables.forEach {
                it.place( 0, 0)
            }
            ci.topDrawerPlacables.forEach {
                it.place(0, 0)
            }
            ci.mainPlaceables.forEach {
                it.place(drawerDp.roundToPx(), drawerDp.roundToPx())
            }
        }
    }
}

private enum class DraweredContentLayoutSlots {
    TopDrawer,
    BottomDrawer,
    MainContent,
    LeftDrawer,
    RightDrawer
}
