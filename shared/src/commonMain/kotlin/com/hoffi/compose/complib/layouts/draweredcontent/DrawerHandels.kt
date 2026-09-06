package com.hoffi.compose.complib.layouts.draweredcontent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


@Composable
fun TopDrawerHandle(
    modifier: Modifier = Modifier,
    handleHeight: Dp = DrawerHandleDefaults.drawerDp,
    isClosed: Boolean = true,
    ) {
    Surface(
        tonalElevation = DrawerHandleDefaults.drawerTonalElevation,
        shadowElevation = DrawerHandleDefaults.drawerTonalElevation * 2,
        modifier = Modifier
            .fillMaxWidth()
            .height(handleHeight)
    ) {
        Box(Modifier.fillMaxWidth()) {
            DrawerHandleTrianglesUpDown(pointsDown = isClosed, drawerDp = handleHeight)
        }
    }
}

@Composable
fun BottomDrawerHandle(
    modifier: Modifier = Modifier,
    handleHeight: Dp = DrawerHandleDefaults.drawerDp,
    isClosed: Boolean = true,
    ) {
    Surface(
        tonalElevation = DrawerHandleDefaults.drawerTonalElevation,
        shadowElevation = DrawerHandleDefaults.drawerTonalElevation * 2,
        modifier = Modifier
            .fillMaxWidth()
            .height(handleHeight)
    ) {
        Box(Modifier.fillMaxSize()) {
            DrawerHandleTrianglesUpDown(pointsDown = isClosed, drawerDp = handleHeight)
        }
    }
}

@Composable
fun LeftDrawerHandle(
    modifier: Modifier = Modifier,
    handleWidth: Dp = DrawerHandleDefaults.drawerDp,
    isClosed: Boolean = true,
    ) {
    Surface(
        tonalElevation = DrawerHandleDefaults.drawerTonalElevation,
        shadowElevation = DrawerHandleDefaults.drawerTonalElevation * 2,
        modifier = Modifier
            .width(handleWidth)
            .fillMaxHeight()
    ) {
        Box(Modifier.fillMaxSize()) {
            DrawerHandleTrianglesLeftRight(pointsRight = isClosed, drawerDp = handleWidth)
        }
    }
}

@Composable
fun RightDrawerHandle(
    modifier: Modifier = Modifier,
    handleWidth: Dp = DrawerHandleDefaults.drawerDp,
    isClosed: Boolean = true,

    ) {
    Surface(
        tonalElevation = DrawerHandleDefaults.drawerTonalElevation,
        shadowElevation = DrawerHandleDefaults.drawerTonalElevation * 2,
        modifier = Modifier
            .width(handleWidth)
            .fillMaxHeight()
    ) {
        Box(Modifier.fillMaxSize()) {
            DrawerHandleTrianglesLeftRight(pointsRight = isClosed, drawerDp = handleWidth)
        }
    }
}

@Composable
private fun PointsDownTriangle(w: Dp, h: Dp, xOffset: Dp) {
    val stroke = 2.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (xOffset < 0.dp) Alignment.TopEnd else Alignment.TopStart,
    ) {
        Canvas(Modifier.width(w).height(h).offset(x = xOffset)) {
        val triangle = Path().apply {
            moveTo(0f, 0f)
            lineTo(w.toPx(), 0f)
            lineTo(w.toPx() / 2f, h.toPx())
            close()
        }
        drawPath(triangle, color = Color.Transparent, style = Fill)
        drawPath(triangle, color = Color.Black, style = Stroke(width = stroke.toPx()))
        }
    }
}

@Composable
private fun PointsUpTriangle(w: Dp, h: Dp, xOffset: Dp) {
    val stroke = 2.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (xOffset < 0.dp) Alignment.TopEnd else Alignment.TopStart,
    ) {
        Canvas(Modifier.width(w).height(h).offset(x = xOffset)) {
        val triangle = Path().apply {
            moveTo(0f, h.toPx())
            lineTo(w.toPx(), h.toPx())
            lineTo(w.toPx() / 2f, 0f)
            close()
        }
        drawPath(triangle, color = Color.Transparent, style = Fill)
        drawPath(triangle, color = Color.Black, style = Stroke(width = stroke.toPx()))
        }
    }
}

@Composable
private fun PointsRightTriangle(w: Dp, h: Dp, yOffset: Dp) {
    val stroke = 2.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (yOffset < 0.dp) Alignment.BottomStart else Alignment.TopStart,
    ) {
        Canvas(Modifier.width(w).height(h).offset(y = yOffset)) {
            val triangle = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, h.toPx())
                lineTo(w.toPx(), h.toPx() / 2f)
                close()
            }
            drawPath(triangle, color = Color.Transparent, style = Fill)
            drawPath(triangle, color = Color.Black, style = Stroke(width = stroke.toPx()))
        }
    }
}

@Composable
private fun PointsLeftTriangle(w: Dp, h: Dp, yOffset: Dp) {
    val stroke = 2.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (yOffset < 0.dp) Alignment.BottomStart else Alignment.TopStart,
    ) {
        Canvas(Modifier.width(w).height(h).offset(y = yOffset)) {
            val triangle = Path().apply {
                moveTo(w.toPx(), 0f)
                lineTo(w.toPx(), h.toPx())
                lineTo(0f, h.toPx() / 2f)
                close()
            }
            drawPath(triangle, color = Color.Transparent, style = Fill)
            drawPath(triangle, color = Color.Black, style = Stroke(width = stroke.toPx()))
        }
    }
}

@Composable
private fun DrawerHandleTrianglesUpDown(pointsDown: Boolean, drawerDp: Dp) {
    val w = drawerDp * 2
    val offset = 15.dp

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val middleOffset = (maxWidth/2) - drawerDp

        for (atX in listOf(offset, middleOffset, -offset)) {
            if ( pointsDown) {
                PointsDownTriangle(w = w, h = drawerDp, xOffset = atX)
            } else {
                PointsUpTriangle(w = w, h = drawerDp, xOffset = atX)
            }
        }
    }
}

@Composable
private fun DrawerHandleTrianglesLeftRight(pointsRight: Boolean, drawerDp: Dp) {
    val h = drawerDp * 2
    val offset = 15.dp

    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val middleOffset = (maxHeight/2) - drawerDp
        for (atY in listOf(offset, middleOffset, -offset)) {
            if ( pointsRight) {
                PointsRightTriangle(w = drawerDp, h = h, yOffset = atY)
            } else {
                PointsLeftTriangle(w = drawerDp, h = h, yOffset = atY)
            }
        }
    }
}

object DrawerHandleDefaults {
    val drawerDp: Dp = 6.dp
    val drawerTonalElevation: Dp = 3.dp
}
