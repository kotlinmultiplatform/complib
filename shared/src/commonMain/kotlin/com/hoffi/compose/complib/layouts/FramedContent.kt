package com.hoffi.compose.complib.layouts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

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
    mainContent: @Composable (PaddingValues) -> Unit,
    topPanel: @Composable (PaddingValues) -> Unit = {},
    bottomPanel: @Composable (PaddingValues) -> Unit = {},
    leftPanel: @Composable (PaddingValues) -> Unit = {},
    rightPanel: @Composable (PaddingValues) -> Unit = {},
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

        val fci = FramedContentInfo(constraints)

        when (framing) {
            FramingStyle.STRETCH_TOP_AND_BOTTOM -> {
                measureTopAndBottom(fci, topPanel, bottomPanel)

                // side panels eventually are place(x, topPanelHeight) UNDER top and ABOVE bottom
                // for ending ABOVE bottom, they have to be "shortened" in height by a bottom padding
                val sidePanelsPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftPadding = sidePanelsPadding
                fci.rightPadding = sidePanelsPadding

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)

                fci.leftConstraints = fci.leftConstraints.copy(maxHeight=mainContentHeightMax)
                fci.rightConstraints = fci.leftConstraints

                measureLeftAndRight(fci, leftPanel, rightPanel)

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_AND_BOTTOM
                val leftToRight1 = fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width

                val topToBottom1 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.leftSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_AND_BOTTOM
            FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // side panels eventually are place(x, topPanelHeight) UNDER top but stretch fully vertically from there
                // so they need to be "shortened" in height by a bottom padding of topPanel.height, because they will start under topPanel
                val sidePanelsPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.leftPadding = sidePanelsPadding
                fci.rightPadding = sidePanelsPadding

                fci.leftConstraints = fci.leftConstraints.copy(maxHeight=givenMaxHeight - fci.topSize.height)
                fci.rightConstraints = fci.leftConstraints

                measureLeftAndRight(fci, leftPanel, rightPanel)

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = mainContentWidthMax)
                // =================================+
                // measure bottomPanel
                // =================================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the bottomPanel
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                val leftToRight1 = fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.topSize.height + fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
            FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // left panel eventually is place(0, topPanelHeight) UNDER top and stretched fully vertically from there
                // so it needs to be "shortened" in height by a bottom padding of topPanel.height, because it will start under topPanel
                fci.leftPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight=givenMaxHeight - fci.topSize.height)

                // ==============================+
                // measure leftPanel
                // ==============================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel eventually is place(leftPanelWidth, y) UNDER top and mainContent
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start right of leftPanel
                fci.bottomPadding = PaddingValues(end = fci.leftSize.width.toDp())
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.rightPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height - fci.bottomSize.height)

                // =====================================+
                // measure rightPanel
                // =====================================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)


                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
                val leftToRight1 = fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width

                val topToBottom1 = fci.topSize.height  + fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_TOP_THEN_LEFT_THEN_BOTTOM
            FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM -> {
                // ==============================+
                // measure topPanel
                // ==============================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // right panel eventually is place(w - rightPanelWidth, topPanelHeight) UNDER top and stretched fully vertically from there
                // so it needs to be "shortened" in height by a bottom padding of topPanel.height, because it will start under topPanel
                fci.rightPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight=givenMaxHeight - fci.topSize.height)

                // ==============================+
                // measure rightPanel
                // ==============================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel eventually is place(0, y) UNDER top and mainContent
                // so it needs to be "shortened" in width by a end padding of rightPanel.width, because it will end left of rightPanel
                fci.bottomPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.rightSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.leftPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height - fci.bottomSize.height)

                // =====================================+
                // measure leftPanel
                // =====================================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)


                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
                val leftToRight1 = fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.topSize.height  + fci.leftSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_TOP_THEN_RIGHT_THEN_BOTTOM
            FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // side panels eventually are place(x, topPanelHeight) UNDER top but stretch fully vertically from there
                // so they need to be "shortened" in height by a bottom padding of topPanel.height, because they will start under topPanel
                val sidePanelsPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftPadding = sidePanelsPadding
                fci.rightPadding = sidePanelsPadding

                fci.leftConstraints = fci.leftConstraints.copy(maxHeight=givenMaxHeight - fci.bottomSize.height)
                fci.rightConstraints = fci.leftConstraints

                measureLeftAndRight(fci, leftPanel, rightPanel)

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.topConstraints = fci.topConstraints.copy(maxWidth = mainContentWidthMax)
                // =================================+
                // measure topPanel
                // =================================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the bottomPanel
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
                val leftToRight2 = fci.leftSize.width + fci.topSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight1 = fci.bottomSize.width

                val topToBottom1 = fci.leftSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_LEFT_AND_RIGHT
            FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // left panel eventually is place(0, 0)
                // bit it needs to be "shortened" in height by a bottom padding of bottomPanel.height, because it will over bottomPanel
                fci.leftPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight=givenMaxHeight - fci.bottomSize.height)

                // ==============================+
                // measure leftPanel
                // ==============================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top panel eventually is place(leftWidth, 0)
                // but it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start right of leftPanel
                fci.topPadding = PaddingValues(end = fci.leftSize.width.toDp())
                fci.topConstraints = fci.topConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.rightPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height - fci.bottomSize.height)

                // =====================================+
                // measure rightPanel
                // =====================================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
                val leftToRight1 = fci.leftSize.width + fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width

                val topToBottom1 = fci.leftSize.height  + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_LEFT_THEN_TOP
            FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP -> {
                // ==============================+
                // measure bottomPanel
                // ==============================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.rightPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight=givenMaxHeight - fci.bottomSize.height)

                // ==============================+
                // measure rightPanel
                // ==============================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.topPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.rightSize.width)

                // ==============================+
                // measure topPanel
                // =================================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.leftPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height - fci.bottomSize.height)

                // =====================================+
                // measure leftPanel
                // =====================================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH _and_ HEIGHT of the mainContent
                val mainContentMaxHeight = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                val mainContentMaxWidth = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)


                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentMaxHeight, maxWidth = mainContentMaxWidth)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
                val leftToRight1 = fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width

                val topToBottom1 = fci.topSize.height  + fci.leftSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_BOTTOM_THEN_RIGHT_THEN_TOP
            FramingStyle.STRETCH_LEFT_AND_RIGHT -> {
                measureLeftAndRight(fci, leftPanel, rightPanel)

                // top/bottom panels eventually are place(x,0) right of left and left of right
                // for ending (leftWidth, 0), (leftWidth, height - bottomHeight) they have to be "shortened" in width by an end padding
                val topBottomPanelsPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topPadding = topBottomPanelsPadding
                fci.bottomPadding = topBottomPanelsPadding

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.topConstraints = fci.topConstraints.copy(maxWidth=mainContentWidthMax)
                fci.bottomConstraints = fci.topConstraints

                measureTopAndBottom(fci, topPanel, bottomPanel)

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                val leftToRight1 = fci.leftSize.width + fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_TOP_THEN_LEFT_AND_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // FramingStyle.STRETCH_LEFT_AND_RIGHT
            FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top and bottom panels eventually are place(leftPanelWidth, 0) RIGHT of leftPanel but stretch fully horizontally from there
                // so they need to be "shortened" in width by a end padding of leftPanel.width, because they will start after leftPanel
                val topAndBottomPanelsPadding = PaddingValues(end = fci.leftSize.width.toDp())
                fci.topPadding = topAndBottomPanelsPadding
                fci.bottomPadding = topAndBottomPanelsPadding

                fci.topConstraints = fci.topConstraints.copy(maxWidth=givenMaxWidth - fci.leftSize.width)
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width)

                measureTopAndBottom(fci, topPanel, bottomPanel)

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)

                fci.rightPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight = mainContentHeightMax)
                // =================================+
                // measure rightPanel
                // =================================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = fci.leftSize.width + fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width

                val topToBottom1 = fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_TOP_AND_BOTTOM
            FramingStyle.STRETCH_LEFT_THEN_BOTTOM_THEN_RIGHT -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // bottom panel is eventually place(leftPanelWidth, 0) RIGHT of leftPanel but stretched fully horizontally from there
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start after leftPanel
                fci.bottomPadding = PaddingValues(end = fci.leftSize.width.toDp())
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )


                fci.rightPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight = givenMaxHeight - fci.bottomSize.height)
                // =================================+
                // measure rightPanel
                // =================================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.topPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topConstraints = fci.topConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width - fci.rightSize.width)

                // ==============================+
                // measure topPanel
                // ==============================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max Height of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = fci.leftSize.width + fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width

                val topToBottom1 = fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height + fci.bottomSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_BOTTOM_THEN_RIGHT
            FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT -> {
                // ==============================+
                // measure leftPanel
                // ==============================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top panel is eventually place(leftPanelWidth, 0) RIGHT of leftPanel but stretched fully horizontally from there
                // so it needs to be "shortened" in width by a end padding of leftPanel.width, because it will start after leftPanel
                fci.topPadding = PaddingValues(end = fci.leftSize.width.toDp())
                fci.topConstraints = fci.topConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )


                fci.rightPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.rightConstraints = fci.rightConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height)
                // =================================+
                // measure rightPanel
                // =================================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.bottomPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.leftSize.width - fci.rightSize.width)

                // ==============================+
                // measure bottomPanel
                // ==============================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max Height of the mainContent, having complete Constraints for the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT
                val leftToRight1 = fci.leftSize.width + fci.topSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.topSize.height + fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_LEFT_THEN_TOP_THEN_RIGHT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, fci.topSize.height)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_LEFT_THEN_TOP_THEN_RIGHT
            FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // top and bottom panels eventually are place(leftPanelWidth, 0) RIGHT of leftPanel but stretch fully horizontally from there
                // so they need to be "shortened" in width by a end padding of rightPanel.width, because they will end before rightPanel
                val topAndBottomPanelsPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topPadding = topAndBottomPanelsPadding
                fci.bottomPadding = topAndBottomPanelsPadding

                fci.topConstraints = fci.topConstraints.copy(maxWidth=givenMaxWidth - fci.rightSize.width)
                fci.bottomConstraints = fci.bottomConstraints.copy(maxWidth = givenMaxWidth - fci.rightSize.width)

                measureTopAndBottom(fci, topPanel, bottomPanel)

                // now we know the max HEIGHT of the mainContent
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)

                fci.leftPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight = mainContentHeightMax)
                // =================================+
                // measure leftPanel
                // =================================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.mainPadding = PaddingValues(start = fci.leftSize.width.toDp(), bottom = fci.bottomSize.height.toDp(), end = fci.rightSize.width.toDp())
                // and finally we know the max WIDTH of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
                val leftToRight1 = fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.topSize.height + fci.leftSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_TOP_AND_BOTTOM
            FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.bottomPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.bottomConstraints = fci.topConstraints.copy(maxWidth=givenMaxWidth - fci.rightSize.width)

                // =================================+
                // measure bottomPanel
                // =================================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.leftPadding = PaddingValues(bottom = fci.bottomSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight = givenMaxHeight - fci.bottomSize.height)
                // =================================+
                // measure leftPanel
                // =================================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.topPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topConstraints = fci.topConstraints.copy(maxWidth=mainContentWidthMax)

                // ==============================+
                // measure topPanel
                // ==============================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainPadding = PaddingValues(end = fci.rightSize.width.toDp(), bottom = fci.bottomSize.height.toDp())
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
                val leftToRight1 = fci.leftSize.width + fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.leftSize.height + fci.bottomSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(0, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, 0)
                    }
                    fci.topPlaceables.forEach {
                        it.place(fci.leftSize.width, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_BOTTOM_THEN_LEFT
            FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT -> {
                // ==============================+
                // measure rightPanel
                // ==============================+
                fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
                    rightPanel(fci.rightPadding)
                }.map { it.measure(fci.rightConstraints) }
                fci.rightSize = IntSize(
                    width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.topPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.topConstraints = fci.topConstraints.copy(maxWidth=givenMaxWidth - fci.rightSize.width)

                // =================================+
                // measure topPanel
                // =================================+
                fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
                    topPanel(fci.topPadding)
                }.map { it.measure(fci.topConstraints) }
                fci.topSize = IntSize(
                    width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
                )

                fci.leftPadding = PaddingValues(bottom = fci.topSize.height.toDp())
                fci.leftConstraints = fci.leftConstraints.copy(maxHeight = givenMaxHeight - fci.topSize.height)
                // =================================+
                // measure leftPanel
                // =================================+
                fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
                    leftPanel(fci.leftPadding)
                }.map { it.measure(fci.leftConstraints) }
                fci.leftSize = IntSize(
                    width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // now we know the max WIDTH of the mainContent
                val mainContentWidthMax = (givenMaxWidth - fci.leftSize.width - fci.rightSize.width).coerceAtLeast(0)

                fci.bottomPadding = PaddingValues(end = fci.rightSize.width.toDp())
                fci.bottomConstraints = fci.topConstraints.copy(maxWidth=mainContentWidthMax)

                // ==============================+
                // measure bottomPanel
                // ==============================+
                fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
                    bottomPanel(fci.bottomPadding)
                }.map { it.measure(fci.bottomConstraints) }
                fci.bottomSize = IntSize(
                    width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
                    height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
                )

                // and finally we know the max HEIGHT of the mainContent, having complete Constraints for the mainContent
                // and therefore also for the rightPanel
                val mainContentHeightMax = (givenMaxHeight - fci.topSize.height - fci.bottomSize.height).coerceAtLeast(0)
                fci.mainPadding = PaddingValues(end = fci.rightSize.width.toDp(), bottom = fci.bottomSize.height.toDp())
                fci.mainConstraints = fci.mainConstraints.copy(maxHeight = mainContentHeightMax, maxWidth = mainContentWidthMax)

                measureMainContent(fci, mainContent)

                // ==========================================================================================================+
                // so now we can determine the overall (biggest combination) size of our complete FramedContent
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT
                val leftToRight1 = fci.topSize.width + fci.rightSize.width
                val leftToRight2 = fci.leftSize.width + fci.mainSize.width + fci.rightSize.width
                val leftToRight3 = fci.leftSize.width + fci.bottomSize.width + fci.rightSize.width

                val topToBottom1 = fci.topSize.height + fci.leftSize.height
                val topToBottom2 = fci.topSize.height + fci.mainSize.height + fci.bottomSize.height
                val topToBottom3 = fci.rightSize.height

                val eventualLayoutWidth = maxOf(leftToRight1, leftToRight2, leftToRight3)
                val eventualLayoutHeight = maxOf(topToBottom1, topToBottom2, topToBottom3)

                // ==========================================================================================================+
                // and place the subcomposed (pre rendered and padded) Placeables into the layout
                // ==========================================================================================================+
                // FramingStyle.STRETCH_RIGHT_THEN_TOP_THEN_LEFT
                layout(eventualLayoutWidth, eventualLayoutHeight) {
                    // The bottom bar is always at the bottom of the layout
                    fci.bottomPlaceables.forEach {
                        it.place(fci.leftSize.width, eventualLayoutHeight - fci.bottomSize.height)
                    }
                    fci.rightPlaceables.forEach {
                        it.place( eventualLayoutWidth - fci.rightSize.width, 0)
                    }
                    fci.leftPlaceables.forEach {
                        it.place( 0, fci.topSize.height)
                    }
                    fci.topPlaceables.forEach {
                        it.place(0, 0)
                    }
                    fci.mainPlaceables.forEach {
                        it.place(fci.leftSize.width, fci.topSize.height)
                    }
                }
            } // STRETCH_RIGHT_THEN_TOP_THEN_LEFT
        } // when
    } // SubcomposeLayout
}

private fun SubcomposeMeasureScope.measureLeftAndRight(
    fci: FramedContentInfo,
    leftPanel: @Composable ((PaddingValues) -> Unit),
    rightPanel: @Composable ((PaddingValues) -> Unit),
) {
    fci.leftPlaceables = subcompose(FramedContentLayoutSlots.LeftPanel) {
        leftPanel(fci.leftPadding)
    }.map { it.measure(fci.leftConstraints) }
    fci.leftSize = IntSize(
        width = fci.leftPlaceables.maxOfOrNull { it.width } ?: 0,
        height = fci.leftPlaceables.maxOfOrNull { it.height } ?: 0
    )
    // =====================================+
    // measure rightPanel
    // =====================================+
    fci.rightPlaceables = subcompose(FramedContentLayoutSlots.RightPanel) {
        rightPanel(fci.rightPadding)
    }.map { it.measure(fci.rightConstraints) }
    fci.rightSize = IntSize(
        width = fci.rightPlaceables.maxOfOrNull { it.width } ?: 0,
        height = fci.rightPlaceables.maxOfOrNull { it.height } ?: 0
    )
}

private fun SubcomposeMeasureScope.measureTopAndBottom(
    fci: FramedContentInfo,
    topPanel: @Composable ((PaddingValues) -> Unit),
    bottomPanel: @Composable ((PaddingValues) -> Unit),
) {
    // ==============================+
    // measure topPanel
    // ==============================+
    fci.topPlaceables = subcompose(FramedContentLayoutSlots.TopPanel) {
        topPanel(fci.topPadding)
    }.map { it.measure(fci.topConstraints) }
    fci.topSize = IntSize(
        width = fci.topPlaceables.maxOfOrNull { it.width } ?: 0,
        height = fci.topPlaceables.maxOfOrNull { it.height } ?: 0
    )

    // =================================+
    // measure bottomPanel
    // =================================+
    fci.bottomPlaceables = subcompose(FramedContentLayoutSlots.BottomPanel) {
        bottomPanel(fci.bottomPadding)
    }.map { it.measure(fci.bottomConstraints) }
    fci.bottomSize = IntSize(
        width = fci.bottomPlaceables.maxOfOrNull { it.width } ?: 0,
        height = fci.bottomPlaceables.maxOfOrNull { it.height } ?: 0
    )
}


private fun SubcomposeMeasureScope.measureMainContent(
    fci: FramedContentInfo,
    mainContent: @Composable ((PaddingValues) -> Unit),
) {
    // ====================================+
    // measure mainContent
    // ====================================+
    fci.mainPlaceables = subcompose(FramedContentLayoutSlots.MainContent) {
        mainContent(fci.mainPadding)
    }.map { it.measure(fci.mainConstraints) }
    fci.mainSize = IntSize(
        width = fci.mainPlaceables.maxOfOrNull { it.width } ?: 0,
        height = fci.mainPlaceables.maxOfOrNull { it.height } ?: 0
    )
}

private enum class FramedContentLayoutSlots { TopPanel, BottomPanel, MainContent, LeftPanel, RightPanel }
