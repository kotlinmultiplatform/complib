package com.hoffi.compose.complib.layouts.framedcontent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import com.hoffi.compose.complib.layouts.EmptyComposable

enum class FramingStyle {
    // TOP
    STRETCH_TOP_AND_BOTTOM,
    STRETCH_TOP_THEN_LEFT_AND_RIGHT,
    STRETCH_TOP_THEN_LEFT_THEN_BOTTOM,
    STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM,

    // BOTTOM
    STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT,
    STRETCH_BOTTOM_THEN_LEFT_THEN_TOP,
    STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP,

    // LEFT
    STRETCH_LEFT_AND_RIGHT,
    STRETCH_LEFT_THEN_TOP_AND_BOTTOM,
    STRETCH_LEFT_THEN_BOTTOM_THEN_RIGHT,
    STRETCH_LEFT_THEN_TOP_THEN_RIGHT,

    // RIGHT
    STRETCH_RIGHT_THEN_TOP_AND_BOTTOM,
    STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT,
    STRETCH_RIGHT_THEN_TOP_THEN_LEFT;
}

/**
 * Holds the measured Placeables and Constraints for each panel and the main content.
 *
 * A Constraints object describes what sizes a child _may_ be, while a Placeable describes what size a child _is_.
 */
class FramedContentInfo(parentConstraints: Constraints, initialPadding: PaddingValues = PaddingValues()) {

    var mainPlaceables: List<Placeable> = emptyList()
    var topPlaceables: List<Placeable> = emptyList()
    var bottomPlaceables: List<Placeable> = emptyList()
    var leftPlaceables: List<Placeable> = emptyList()
    var rightPlaceables: List<Placeable> = emptyList()

    private val initialChildConstraints: Constraints = parentConstraints.copy(minWidth = 0, minHeight = 0)
    var mainConstraints: Constraints = initialChildConstraints
    var topConstraints: Constraints = initialChildConstraints
    var bottomConstraints: Constraints = initialChildConstraints
    var leftConstraints: Constraints = initialChildConstraints
    var rightConstraints: Constraints = initialChildConstraints

    var mainPadding: PaddingValues = initialPadding
    var topPadding: PaddingValues = initialPadding
    var bottomPadding: PaddingValues = initialPadding
    var leftPadding: PaddingValues = initialPadding
    var rightPadding: PaddingValues = initialPadding

    var mainSize: IntSize = IntSize.Zero
    var topSize: IntSize = IntSize.Zero
    var bottomSize: IntSize = IntSize.Zero
    var leftSize: IntSize = IntSize.Zero
    var rightSize: IntSize = IntSize.Zero
}

/**
 * Arranges a central content area with optional top/bottom/left/right panels.
 *
 * Note: the [PaddingValues] provided to the content and panel lambdas are usually ignored by the Composables placed inside this layout,
 * because the panel placement is driven by the FramedContent layout itself. A few container composables do
 * honor padding values though, such as [androidx.compose.material3.Scaffold], navigation drawers, and ModalBottomSheet / DrawerSheet variants.
 */
@Composable
fun FramedContent(
    modifier: Modifier = Modifier,
    framing: FramingStyle = FramingStyle.STRETCH_TOP_AND_BOTTOM,
    topPanel: @Composable (PaddingValues) -> Unit = EmptyComposable,
    bottomPanel: @Composable (PaddingValues) -> Unit = EmptyComposable,
    leftPanel: @Composable (PaddingValues) -> Unit = EmptyComposable,
    rightPanel: @Composable (PaddingValues) -> Unit = EmptyComposable,
    mainContent: @Composable (PaddingValues) -> Unit,
) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier) {
            FramedContentLayout(
                framing = framing,
                mainContent = mainContent,
                topPanel = topPanel,
                bottomPanel = bottomPanel,
                leftPanel = leftPanel,
                rightPanel = rightPanel,
            )
        }
    }

    // if something to draw over the FramedContent, e.g. a ModalDrawer, it comes here

    child(modifier)
}

@Composable
private fun FramedContentLayout(
    framing: FramingStyle = FramingStyle.STRETCH_TOP_AND_BOTTOM,
    mainContent: @Composable (PaddingValues) -> Unit,
    topPanel: @Composable (PaddingValues) -> Unit,
    bottomPanel: @Composable (PaddingValues) -> Unit,
    leftPanel: @Composable (PaddingValues) -> Unit,
    rightPanel: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val givenMaxWidth = constraints.maxWidth
        val givenMaxHeight = constraints.maxHeight

        // ==================================================================================================================+
        // do some health asserts if FramedContent is placed inside a Component with INFINITE width and/or height
        // ==================================================================================================================+
        val unboundedInfinities = mutableListOf<String>()
        if ( !constraints.hasBoundedWidth && (givenMaxWidth >= (Int.MAX_VALUE/2) || givenMaxWidth <= (Int.MIN_VALUE/2)) ) {
            unboundedInfinities.add("FramedContent parent has unbounded width")
        }
        if ( !constraints.hasBoundedHeight && (givenMaxHeight >= (Int.MAX_VALUE/2) || givenMaxHeight <= (Int.MIN_VALUE/2)) ) {
            unboundedInfinities.add("FramedContent parent has unbounded height")
        }
        if (unboundedInfinities.isNotEmpty()) {
            throw Exception("FramedContent ${unboundedInfinities.joinToString()}")
        }

        // ==================================================================================================================+
        // measure each Panel (only once!!!) with pre-calculated maxWidth/Height constraints after they have been rendered
        // ==================================================================================================================+

        val ci = FramedContentInfo(constraints)

        when (framing) {
            FramingStyle.STRETCH_TOP_AND_BOTTOM -> {
                measureTopAndBottom(ci, topPanel, bottomPanel)

                // side panels eventually are place(x, topPanelHeight) UNDER top and ABOVE bottom
                // for ending ABOVE bottom, they have to be "shortened" in height by a bottom padding
                val sidePanelsPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftPadding = sidePanelsPadding
                ci.rightPadding = sidePanelsPadding

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)

                ci.leftConstraints = ci.leftConstraints.copy(maxHeight=mainContentHeightMax)
                ci.rightConstraints = ci.leftConstraints

                measureLeftAndRight(ci, leftPanel, rightPanel)

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_AND_BOTTOM
                val leftToRight1 = ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width

                val topToBottom1 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.leftSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_AND_BOTTOM
            FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // side panels eventually are place(x, topPanelHeight) UNDER top but stretch fully vertically from there
                // so they need to be "shortened" in height by a bottom padding of topPanel.height, because they will start under topPanel
                val sidePanelsPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.leftPadding = sidePanelsPadding
                ci.rightPadding = sidePanelsPadding

                ci.leftConstraints = ci.leftConstraints.copy(maxHeight=givenMaxHeight - ci.topSize.height)
                ci.rightConstraints = ci.leftConstraints

                measureLeftAndRight(ci, leftPanel, rightPanel)

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = mainContentWidthMax)
                // =================================+
                // measure bottomPanel
                // =================================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the bottomPanel
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                val leftToRight1 = ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.topSize.height + ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
            FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // left panel eventually is place(0, topPanelHeight) UNDER top and stretched fully vertically from there
                // so it needs to be "shortened" in height by a bottom padding of topPanel.height, because it will start under topPanel
                ci.leftPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight=givenMaxHeight - ci.topSize.height)

                // ==============================+
                // measure leftPanel
                // ==============================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel eventually is place(leftPanelWidth, y) UNDER top and mainContent
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start right of leftPanel
                ci.bottomPadding = PaddingValues(end = ci.leftSize.width.toDp())
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.rightPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height - ci.bottomSize.height)

                // =====================================+
                // measure rightPanel
                // =====================================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)


                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
                val leftToRight1 = ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width

                val topToBottom1 = ci.topSize.height  + ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
            FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // right panel eventually is place(w - rightPanelWidth, topPanelHeight) UNDER top and stretched fully vertically from there
                // so it needs to be "shortened" in height by a bottom padding of topPanel.height, because it will start under topPanel
                ci.rightPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight=givenMaxHeight - ci.topSize.height)

                // ==============================+
                // measure rightPanel
                // ==============================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel eventually is place(0, y) UNDER top and mainContent
                // so it needs to be "shortened" in width by a end padding of rightPanel.width, because it will end left of rightPanel
                ci.bottomPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.rightSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.leftPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height - ci.bottomSize.height)

                // =====================================+
                // measure leftPanel
                // =====================================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)


                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
                val leftToRight1 = ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.topSize.height  + ci.leftSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
            FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // side panels eventually are place(x, topPanelHeight) UNDER top but stretch fully vertically from there
                // so they need to be "shortened" in height by a bottom padding of topPanel.height, because they will start under topPanel
                val sidePanelsPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftPadding = sidePanelsPadding
                ci.rightPadding = sidePanelsPadding

                ci.leftConstraints = ci.leftConstraints.copy(maxHeight=givenMaxHeight - ci.bottomSize.height)
                ci.rightConstraints = ci.leftConstraints

                measureLeftAndRight(ci, leftPanel, rightPanel)

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.topConstraints = ci.topConstraints.copy(maxWidth = mainContentWidthMax)
                // =================================+
                // measure topPanel
                // =================================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the bottomPanel
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
                val leftToRight2 = ci.leftSize.width + ci.topSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight1 = ci.bottomSize.width

                val topToBottom1 = ci.leftSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
            FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // left panel eventually is place(0, 0)
                // bit it needs to be "shortened" in height by a bottom padding of bottomPanel.height, because it will over bottomPanel
                ci.leftPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight=givenMaxHeight - ci.bottomSize.height)

                // ==============================+
                // measure leftPanel
                // ==============================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top panel eventually is place(leftWidth, 0)
                // but it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start right of leftPanel
                ci.topPadding = PaddingValues(end = ci.leftSize.width.toDp())
                ci.topConstraints = ci.topConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.rightPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height - ci.bottomSize.height)

                // =====================================+
                // measure rightPanel
                // =====================================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
                val leftToRight1 = ci.leftSize.width + ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width

                val topToBottom1 = ci.leftSize.height  + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
            FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.rightPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight=givenMaxHeight - ci.bottomSize.height)

                // ==============================+
                // measure rightPanel
                // ==============================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.topPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.rightSize.width)

                // ==============================+
                // measure topPanel
                // =================================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.leftPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height - ci.bottomSize.height)

                // =====================================+
                // measure leftPanel
                // =====================================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)


                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
                val leftToRight1 = ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width

                val topToBottom1 = ci.topSize.height  + ci.leftSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
            FramingStyle.STRETCH_LEFT_AND_RIGHT -> {
                measureLeftAndRight(ci, leftPanel, rightPanel)

                // top/bottom panels eventually are place(x,0) right of left and left of right
                // for ending (leftWidth, 0), (leftWidth, height - bottomHeight) they have to be "shortened" in width by an end padding
                val topBottomPanelsPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topPadding = topBottomPanelsPadding
                ci.bottomPadding = topBottomPanelsPadding

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.topConstraints = ci.topConstraints.copy(maxWidth=mainContentWidthMax)
                ci.bottomConstraints = ci.topConstraints

                measureTopAndBottom(ci, topPanel, bottomPanel)

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                val leftToRight1 = ci.leftSize.width + ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_LEFT_AND_RIGHT
            FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top and bottom panels eventually are place(leftPanelWidth, 0) RIGHT of leftPanel but stretch fully horizontally from there
                // so they need to be "shortened" in width by a end padding of leftPanel.width, because they will start after leftPanel
                val topAndBottomPanelsPadding = PaddingValues(end = ci.leftSize.width.toDp())
                ci.topPadding = topAndBottomPanelsPadding
                ci.bottomPadding = topAndBottomPanelsPadding

                ci.topConstraints = ci.topConstraints.copy(maxWidth=givenMaxWidth - ci.leftSize.width)
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width)

                measureTopAndBottom(ci, topPanel, bottomPanel)

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)

                ci.rightPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight = mainContentHeightMax)
                // =================================+
                // measure rightPanel
                // =================================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = ci.leftSize.width + ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width

                val topToBottom1 = ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_TOP_AND_BOTTOM
            FramingStyle.STRETCH_LEFT_THEN_BOTTOM_THEN_RIGHT -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel is eventually place(leftPanelWidth, 0) RIGHT of leftPanel but stretched fully horizontally from there
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start after leftPanel
                ci.bottomPadding = PaddingValues(end = ci.leftSize.width.toDp())
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )


                ci.rightPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight = givenMaxHeight - ci.bottomSize.height)
                // =================================+
                // measure rightPanel
                // =================================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.topPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topConstraints = ci.topConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width - ci.rightSize.width)

                // ==============================+
                // measure topPanel
                // ==============================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max Height of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = ci.leftSize.width + ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width

                val topToBottom1 = ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height + ci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_BOTTOM_THEN_RIGHT
            FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top panel is eventually place(leftPanelWidth, 0) RIGHT of leftPanel but stretched fully horizontally from there
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start after leftPanel
                ci.topPadding = PaddingValues(end = ci.leftSize.width.toDp())
                ci.topConstraints = ci.topConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )


                ci.rightPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.rightConstraints = ci.rightConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height)
                // =================================+
                // measure rightPanel
                // =================================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.bottomPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.leftSize.width - ci.rightSize.width)

                // ==============================+
                // measure bottomPanel
                // ==============================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max Height of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT
                val leftToRight1 = ci.leftSize.width + ci.topSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.topSize.height + ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, ci.topSize.height)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_TOP_THEN_RIGHT
            FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top and bottom panels eventually are place(leftPanelWidth, 0) RIGHT of leftPanel but stretch fully horizontally from there
                // so they need to be "shortened" in width by a end padding of rightPanel.width, because they will end before rightPanel
                val topAndBottomPanelsPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topPadding = topAndBottomPanelsPadding
                ci.bottomPadding = topAndBottomPanelsPadding

                ci.topConstraints = ci.topConstraints.copy(maxWidth=givenMaxWidth - ci.rightSize.width)
                ci.bottomConstraints = ci.bottomConstraints.copy(maxWidth = givenMaxWidth - ci.rightSize.width)

                measureTopAndBottom(ci, topPanel, bottomPanel)

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)

                ci.leftPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight = mainContentHeightMax)
                // =================================+
                // measure leftPanel
                // =================================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.mainPadding = PaddingValues(start = ci.leftSize.width.toDp(), bottom = ci.bottomSize.height.toDp(), end = ci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.topSize.height + ci.leftSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
            FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.bottomPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.bottomConstraints = ci.topConstraints.copy(maxWidth=givenMaxWidth - ci.rightSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.leftPadding = PaddingValues(bottom = ci.bottomSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight = givenMaxHeight - ci.bottomSize.height)
                // =================================+
                // measure leftPanel
                // =================================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.topPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topConstraints = ci.topConstraints.copy(maxWidth=mainContentWidthMax)

                // ==============================+
                // measure topPanel
                // ==============================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainPadding = PaddingValues(end = ci.rightSize.width.toDp(), bottom = ci.bottomSize.height.toDp())
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
                val leftToRight1 = ci.leftSize.width + ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.leftSize.height + ci.bottomSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    ci.topPlaceables.forEach {
                        it.place(ci.leftSize.width, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
            FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(ci.rightPadding)
                }.map { it.measure(ci.rightConstraints) }
                ci.rightSize = IntSize(
                    width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.topPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.topConstraints = ci.topConstraints.copy(maxWidth=givenMaxWidth - ci.rightSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(ci.topPadding)
                }.map { it.measure(ci.topConstraints) }
                ci.topSize = IntSize(
                    width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                ci.leftPadding = PaddingValues(bottom = ci.topSize.height.toDp())
                ci.leftConstraints = ci.leftConstraints.copy(maxHeight = givenMaxHeight - ci.topSize.height)
                // =================================+
                // measure leftPanel
                // =================================+
                ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(ci.leftPadding)
                }.map { it.measure(ci.leftConstraints) }
                ci.leftSize = IntSize(
                    width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - ci.leftSize.width - ci.rightSize.width).coerceAtLeast(0)

                ci.bottomPadding = PaddingValues(end = ci.rightSize.width.toDp())
                ci.bottomConstraints = ci.topConstraints.copy(maxWidth=mainContentWidthMax)

                // ==============================+
                // measure bottomPanel
                // ==============================+
                ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(ci.bottomPadding)
                }.map { it.measure(ci.bottomConstraints) }
                ci.bottomSize = IntSize(
                    width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentHeightMax = (givenMaxHeight - ci.topSize.height - ci.bottomSize.height).coerceAtLeast(0)
                ci.mainPadding = PaddingValues(end = ci.rightSize.width.toDp(), bottom = ci.bottomSize.height.toDp())
                ci.mainConstraints = ci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(ci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT
                val leftToRight1 = ci.topSize.width + ci.rightSize.width
                val leftToRight2 = ci.leftSize.width + ci.mainSize.width + ci.rightSize.width
                val leftToRight3 = ci.leftSize.width + ci.bottomSize.width + ci.rightSize.width

                val topToBottom1 = ci.topSize.height + ci.leftSize.height
                val topToBottom2 = ci.topSize.height + ci.mainSize.height + ci.bottomSize.height
                val topToBottom3 = ci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    ci.bottomPlaceables.forEach {
                        it.place(ci.leftSize.width, eventualLayoutHeight - ci.bottomSize.height)
                    }
                    ci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - ci.rightSize.width, 0)
                    }
                    ci.leftPlaceables.forEach {
                        it.place( 0, ci.topSize.height)
                    }
                    ci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    ci.mainPlaceables.forEach {
                        it.place(ci.leftSize.width, ci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_TOP_THEN_LEFT
        } // when
    } // SubcomposeLayout
}

private fun SubcomposeMeasureScope.measureLeftAndRight(
    ci: FramedContentInfo,
    leftPanel: @Composable ((PaddingValues) -> Unit),
    rightPanel: @Composable ((PaddingValues) -> Unit),
) {
    ci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
        leftPanel(ci.leftPadding)
    }.map { it.measure(ci.leftConstraints) }
    ci.leftSize = IntSize(
        width = ci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
        height = ci.leftPlaceables.maxOfOrNull { it.height } ?: 0
    )
    // =====================================+
    // measure rightPanel
    // =====================================+
    ci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
        rightPanel(ci.rightPadding)
    }.map { it.measure(ci.rightConstraints) }
    ci.rightSize = IntSize(
        width = ci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
        height = ci.rightPlaceables.maxOfOrNull { it.height } ?: 0
    )
}

private fun SubcomposeMeasureScope.measureTopAndBottom(
    ci: FramedContentInfo,
    topPanel: @Composable ((PaddingValues) -> Unit),
    bottomPanel: @Composable ((PaddingValues) -> Unit),
) {
    // ==============================+
    // measure topPanel
    // ==============================+
    ci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
        topPanel(ci.topPadding)
    }.map { it.measure(ci.topConstraints) }
    ci.topSize = IntSize(
        width = ci.topPlaceables.maxOfOrNull { it.width } ?: 0,
        height = ci.topPlaceables.maxOfOrNull { it.height } ?: 0
    )

    // =================================+
    // measure bottomPanel
    // =================================+
    ci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
        bottomPanel(ci.bottomPadding)
    }.map { it.measure(ci.bottomConstraints) }
    ci.bottomSize = IntSize(
        width = ci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
        height = ci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
    )
}


private fun SubcomposeMeasureScope.measureMainContent(
    ci: FramedContentInfo,
    mainContent: @Composable ((PaddingValues) -> Unit),
) {
    // ====================================+
    // measure mainContent
    // ====================================+
    ci.mainPlaceables = subcompose(FramedContentLayoutSlots.MainContent) {
        mainContent(ci.mainPadding)
    }.map { it.measure(ci.mainConstraints) }
    ci.mainSize = IntSize(
        width = ci.mainPlaceables.maxOfOrNull { it.width } ?: 0,
        height = ci.mainPlaceables.maxOfOrNull { it.height } ?: 0
    )
}

private enum class FramedContentLayoutSlots { TopPanel, BottomPanel, MainContent, LeftPanel, RightPanel }
