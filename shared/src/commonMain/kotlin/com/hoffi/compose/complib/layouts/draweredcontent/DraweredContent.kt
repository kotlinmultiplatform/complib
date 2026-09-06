package com.hoffi.compose.complib.layouts.draweredcontent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.hoffi.compose.complib.layouts.EmptyComposable

class DraweredContentInfo() {
    var mainPlaceables: List<Placeable> = emptyList()

    var topDrawerPlacables: List<Placeable> = emptyList()
    var bottomDrawerPlacables: List<Placeable> = emptyList()
    var leftDrawerPlacables: List<Placeable> = emptyList()
    var rightDrawerPlacables: List<Placeable> = emptyList()

    var topDrawerHandlePlacables: List<Placeable> = emptyList()
    var bottomDrawerHandlePlacables: List<Placeable> = emptyList()
    var leftDrawerHandlePlacables: List<Placeable> = emptyList()
    var rightDrawerHandlePlacables: List<Placeable> = emptyList()

    var topDrawerSize: IntSize = IntSize.Zero
    var bottomDrawerSize: IntSize = IntSize.Zero
    var leftDrawerSize: IntSize = IntSize.Zero
    var rightDrawerSize: IntSize = IntSize.Zero

    var mainSize: IntSize = IntSize.Zero
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
            // ==============================+
            // measure (by composing) top drawerHandle and get its size
            // ==============================+
            ci.topDrawerHandlePlacables = subcompose(DraweredContentLayoutSlots.TopDrawerHandle) {
                TopDrawerHandle(handleHeight = drawerDp)
            }.map { it.measure(constraints.copy(maxHeight = drawerDp.roundToPx())) }
            ci.topDrawerSize = IntSize(
                width = ci.topDrawerHandlePlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.topDrawerHandlePlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (bottomDrawer !== EmptyComposable) {
            // ==============================+
            // measure (by composing) bottom drawerHandle and get its size
            // ==============================+
            ci.bottomDrawerHandlePlacables = subcompose(DraweredContentLayoutSlots.BottomDrawerHandle) {
                BottomDrawerHandle(handleHeight = drawerDp)
            }.map { it.measure(constraints.copy(maxHeight = drawerDp.roundToPx())) }
            ci.bottomDrawerSize = IntSize(
                width = ci.bottomDrawerHandlePlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.bottomDrawerHandlePlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (leftDrawer !== EmptyComposable) {
            // ==============================+
            // measure (by composing) left drawerHandle and get its size
            // ==============================+
            ci.leftDrawerHandlePlacables = subcompose(DraweredContentLayoutSlots.LeftDrawerHandle) {
                LeftDrawerHandle(handleWidth = drawerDp)
            }.map { it.measure(constraints.copy(maxWidth = drawerDp.roundToPx())) }
            ci.leftDrawerSize = IntSize(
                width = ci.leftDrawerHandlePlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.leftDrawerHandlePlacables.maxOfOrNull { it.height } ?: 0
            )
        }
        if (rightDrawer !== EmptyComposable) {
            // ==============================+
            // measure (by composing) right drawerHandle and get its size
            // ==============================+
            ci.rightDrawerHandlePlacables = subcompose(DraweredContentLayoutSlots.RightDrawerHandle) {
                RightDrawerHandle(handleWidth = drawerDp)
            }.map { it.measure(constraints.copy(maxWidth = drawerDp.roundToPx())) }
            ci.rightDrawerSize = IntSize(
                width = ci.rightDrawerHandlePlacables.maxOfOrNull { it.width } ?: 0,
                height = ci.rightDrawerHandlePlacables.maxOfOrNull { it.height } ?: 0
            )
        }

        val mainContentMaxHeight = (givenMaxHeight - ci.topDrawerSize.height - ci.bottomDrawerSize.height).coerceAtLeast(0)
        val mainContentMaxWidth = (givenMaxWidth - ci.leftDrawerSize.width - ci.rightDrawerSize.width).coerceAtLeast(0)

        val mainPadding = PaddingValues(bottom = ci.bottomDrawerSize.height.toDp(), end = ci.rightDrawerSize.width.toDp())
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
            ci.bottomDrawerHandlePlacables.forEach {
                it.place(0, eventualLayoutHeight - ci.bottomDrawerSize.height)
            }
            ci.rightDrawerHandlePlacables.forEach {
                it.place( eventualLayoutWidth - ci.rightDrawerSize.width, 0)
            }
            ci.leftDrawerHandlePlacables.forEach {
                it.place( 0, 0)
            }
            ci.topDrawerHandlePlacables.forEach {
                it.place(ci.leftDrawerSize.width, 0)
            }
            ci.mainPlaceables.forEach {
                it.place(ci.leftDrawerSize.width, ci.topDrawerSize.height)
            }
        }
    }
}

private enum class DraweredContentLayoutSlots {
    TopDrawerHandle, TopDrawer,
    BottomDrawerHandle, BottomDrawer,
    MainContent,
    LeftDrawerHandle, LeftDrawer,
    RightDrawerHandle, RightDrawer
}
