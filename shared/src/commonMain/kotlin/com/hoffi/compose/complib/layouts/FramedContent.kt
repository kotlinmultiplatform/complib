package com.hoffi.compose.complib.layouts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import com.hoffi.compose.complib.printlnErr

@Composable
fun FramedContent(
    modifier: Modifier = Modifier,
    topPanel: @Composable () -> Unit = {},
    bottomPanel: @Composable () -> Unit = {},
    leftPanel: @Composable (PaddingValues) -> Unit = {},
    rightPanel: @Composable (PaddingValues) -> Unit = {},
    mainContent: @Composable (PaddingValues) -> Unit

) {
    val child = @Composable { childModifier: Modifier ->
        Surface(modifier = childModifier) {
            FramedContentLayout(
                topPanel = topPanel,
                bottomPanel = bottomPanel,
                leftPanel = leftPanel,
                rightPanel = rightPanel,
                mainContent = mainContent
            )
        }
    }

    // if something to draw over the FramedContent, e.g. a ModalDrawer, it comes here

    child(modifier)
}

@Composable
private fun FramedContentLayout(
    topPanel: @Composable () -> Unit,
    bottomPanel: @Composable () -> Unit,
    leftPanel: @Composable (PaddingValues) -> Unit,
    rightPanel: @Composable (PaddingValues) -> Unit,
    mainContent: @Composable (PaddingValues) -> Unit
) {
    SubcomposeLayout { constraints ->
        val layoutMaxWidth = constraints.maxWidth
        val layoutMaxHeight = constraints.maxHeight

        // ==========================================================================================================+
        // do some health asserts if FramedContent is placed inside a Component with INFINITE width and/or height
        // ==========================================================================================================+
        val unboundedInfinity = mutableListOf<String>()
        if ( !constraints.hasBoundedWidth && (layoutMaxWidth >= (Int.MAX_VALUE/2) || layoutMaxWidth <= (Int.MIN_VALUE/2)) ) {
            unboundedInfinity.add("unbounded width and width INFINITE")
        }
        if ( !constraints.hasBoundedHeight && (layoutMaxHeight >= (Int.MAX_VALUE/2) || layoutMaxHeight <= (Int.MIN_VALUE/2)) ) {
            unboundedInfinity.add("unbounded height and height INFINITE")
        }
        if (unboundedInfinity.isNotEmpty()) {
            throw Exception("FramedContent ${unboundedInfinity.joinToString()}")
        }

        // ==========================================================================================================+
        // measure each Panel and get back Placeable's for each
        // ==========================================================================================================+

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // topPanel measuring
        val topPanelPlaceables = subcompose(FramedContentLayoutContent.TopPanel, topPanel).map {
            it.measure(looseConstraints)
        }
        val topPanelHeight = topPanelPlaceables.maxByOrNull { it.height }?.height ?: 0
        val topPanelWidth = topPanelPlaceables.maxByOrNull { it.width }?.width ?: 0

        // bottomPanel measuring
        val bottomPanelPlaceables = subcompose(FramedContentLayoutContent.BottomPanel, bottomPanel).map {
            it.measure(looseConstraints)
        }
        val bottomPanelHeight = bottomPanelPlaceables.maxByOrNull { it.height }?.height ?: 0
        val bottomPanelWidth = bottomPanelPlaceables.maxByOrNull { it.width }?.width ?: 0


        val middlePanelsMaxHeight = (layoutMaxHeight - topPanelHeight - bottomPanelHeight).coerceAtLeast(0)
        val sidePanelInnerPadding = PaddingValues(bottom = bottomPanelHeight.toDp())


        // leftPanel measuring
        val leftPanelPlaceables = subcompose(FramedContentLayoutContent.LeftPanel) {
            leftPanel(sidePanelInnerPadding)
        }.map { it.measure(looseConstraints.copy(maxHeight = middlePanelsMaxHeight)) }
        val leftPanelWidth = leftPanelPlaceables.maxByOrNull { it.width }?.width ?: 0
        val leftPanelHeight = leftPanelPlaceables.maxByOrNull { it.height }?.height ?: 0

        // rightPanel measuring
        val rightPanelPlaceables = subcompose(FramedContentLayoutContent.RightPanel) {
            rightPanel(sidePanelInnerPadding)
        }.map { it.measure(looseConstraints.copy(maxHeight = middlePanelsMaxHeight)) }
        val rightPanelWidth = rightPanelPlaceables.maxByOrNull { it.width }?.width ?: 0
        val rightPanelHeight = rightPanelPlaceables.maxByOrNull { it.height }?.height ?: 0


        val mainContentWidthMax = (layoutMaxWidth - leftPanelWidth - rightPanelWidth).coerceAtLeast(0)


        // mainContent measuring
        val mainContentPlaceables = subcompose(FramedContentLayoutContent.MainContent) {
            val innerPadding = PaddingValues(start = leftPanelWidth.toDp(), bottom = bottomPanelHeight.toDp(), end = rightPanelWidth.toDp())
            mainContent(innerPadding)
        }.map { it.measure(looseConstraints.copy(maxHeight = middlePanelsMaxHeight, maxWidth = mainContentWidthMax)) }
        val mainContentWidth = mainContentPlaceables.maxByOrNull { it.width }?.width ?: 0
        val mainContentHeight = mainContentPlaceables.maxByOrNull { it.height }?.height ?: 0


        // ==========================================================================================================+
        // placing
        // ==========================================================================================================+

        val actualLayoutWidth = leftPanelWidth + mainContentWidth + rightPanelWidth
        val maxLayoutWidth = maxOf(actualLayoutWidth, topPanelWidth, bottomPanelWidth)
        val actualLayoutHeight = topPanelHeight + mainContentHeight + bottomPanelHeight
        val maxLayoutHeight = maxOf(actualLayoutHeight, leftPanelHeight, rightPanelHeight)


        // ==========================================================================================================+
        // do some health asserts before actual placing
        // ==========================================================================================================+

        val actualAsserts = mutableListOf<String>()
        if (bottomPanelWidth > actualLayoutWidth) {
            actualAsserts.add("bottomPanel.width <= actualLayoutWidth(=leftWidth+mainWidth+rightWidth)")
        }
        if (topPanelWidth > actualLayoutWidth) {
            actualAsserts.add("topPanel.width <= actualLayoutWidth(=leftWidth+mainWidth+rightWidth)")
        }
        if (leftPanelHeight > actualLayoutHeight) {
            actualAsserts.add("leftPanel.height <= actualLayoutHeight(=topHeight+mainHeight+rightHeight)")
        }
        if (rightPanelHeight > actualLayoutHeight) {
            actualAsserts.add("rightPanel.height <= actualLayoutHeight(=topHeight+mainHeight+rightHeight)")
        }
        if (actualAsserts.isNotEmpty()) {
            printlnErr("Warning: FramedContent asserts failed for: '${actualAsserts.joinToString()}'. (Do you have a Spacer() or .weight(1f) in it?")
        }

        layout(maxLayoutWidth, maxLayoutHeight) {

            mainContentPlaceables.forEach {
                it.place(leftPanelWidth, topPanelHeight)
            }
            leftPanelPlaceables.forEach {
                it.place( 0, topPanelHeight)
            }
            rightPanelPlaceables.forEach {
                it.place( leftPanelWidth + mainContentWidth, topPanelHeight)
            }
            topPanelPlaceables.forEach {
                it.place(0, 0)
            }
            // The bottom bar is always at the bottom of the layout
            bottomPanelPlaceables.forEach {
                it.place(0, topPanelHeight + mainContentHeight)
            }
        }
    }
}

private enum class FramedContentLayoutContent { TopPanel, BottomPanel, MainContent, LeftPanel, RightPanel }
