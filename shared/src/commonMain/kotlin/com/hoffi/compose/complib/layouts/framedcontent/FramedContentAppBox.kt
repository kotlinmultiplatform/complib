package com.hoffi.compose.complib.layouts.framedcontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hoffi.compose.complib.Greeting
import com.hoffi.compose.complib.components.DrawerHandleDefaults.drawerDp
import com.hoffi.compose.complib.components.DrawerAnchor
import com.hoffi.compose.complib.components.DrawerPos
import com.hoffi.compose.complib.debugBorder
import com.hoffi.compose.complib.debugMode
import com.hoffi.compose.complib.components.DraweredContent
import com.hoffi.compose.complib.components.rememberDrawerState
import complib.shared.generated.resources.Res
import complib.shared.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import com.hoffi.compose.complib.EmptyComposable
import kotlin.random.Random

@Composable
fun TopPanel(greeting: String) {
    Box(
        modifier = Modifier.fillMaxWidth().debugBorder(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
@Preview
fun FramedContentAppBox() {
    val greeting = remember { Greeting().greet() }
    debugMode = true
    val framingStyles = FramingStyle.entries
    var currentFraming by remember { mutableStateOf(FramingStyle.STRETCH_TOP_AND_BOTTOM) }
    var topDrawerItems by remember { mutableStateOf(randomExampleDrawerContentItems()) }
    var bottomDrawerItems by remember { mutableStateOf(randomExampleDrawerContentItems()) }
    var leftDrawerItems by remember { mutableStateOf(randomExampleDrawerContentItems()) }
    var rightDrawerItems by remember { mutableStateOf(randomExampleDrawerContentItems()) }
    var activeDrawers by remember { mutableStateOf(DrawerAnchor.entries.toSet()) }

    val drawerInt = with(LocalDensity.current) { drawerDp.roundToPx() }
    val topDrawerState = rememberDrawerState(DrawerAnchor.TOP, drawerInt, DrawerPos.HALF)
    val bottomDrawerState = rememberDrawerState(DrawerAnchor.BOTTOM, drawerInt, DrawerPos.PEEK)
    val leftDrawerState = rememberDrawerState(DrawerAnchor.LEFT, drawerInt, DrawerPos.HALF)
    val rightDrawerState = rememberDrawerState(DrawerAnchor.RIGHT, drawerInt, DrawerPos.HALF)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            Button(
                onClick = {
                    val currentIndex = framingStyles.indexOf(currentFraming)
                    val nextIndex = if (currentIndex == framingStyles.lastIndex) 0 else currentIndex + 1
                    currentFraming = framingStyles[nextIndex]
                },
            ) {
                Text(currentFraming.name)
            }
            Button(
                onClick = {
                    topDrawerItems = randomExampleDrawerContentItems()
                    bottomDrawerItems = randomExampleDrawerContentItems()
                    leftDrawerItems = randomExampleDrawerContentItems()
                    rightDrawerItems = randomExampleDrawerContentItems()
                    activeDrawers = DrawerAnchor.entries
                        .shuffled()
                        .take(Random.nextInt(from = 1, until = DrawerAnchor.entries.size + 1))
                        .toSet()
                    coroutineScope.launch { topDrawerState.snapTo(DrawerPos.entries.random()) }
                    coroutineScope.launch { bottomDrawerState.snapTo(DrawerPos.entries.random()) }
                    coroutineScope.launch { leftDrawerState.snapTo(DrawerPos.entries.random()) }
                    coroutineScope.launch { rightDrawerState.snapTo(DrawerPos.entries.random()) }
                }
            ) {
                Text("Randomize drawers")
            }
        }

        FramedContent(
            modifier = Modifier.fillMaxSize(),
            framing = currentFraming,
            topPanel = { TopPanel(greeting) },
            bottomPanel = {
                Box(
                    modifier = Modifier.fillMaxWidth().debugBorder(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = "Bottom Panel",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            leftPanel = { paddingValues ->
                Box(
                    modifier = Modifier.fillMaxHeight().debugBorder(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Left Panel",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            },
            rightPanel = { paddingValues ->
                Box(
                    modifier = Modifier.fillMaxHeight().debugBorder(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Right Panel",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) {
            // mainContent
            DraweredContent(
                modifier = Modifier.debugBorder(),
                topDrawerState = topDrawerState,
                topDrawer = if (DrawerAnchor.TOP in activeDrawers) {
                    { paddingValues -> ExampleDrawerContent(Orientation.Vertical, "Top Drawer", topDrawerItems) }
                } else EmptyComposable,
                bottomDrawerState = bottomDrawerState,
                bottomDrawer = if (DrawerAnchor.BOTTOM in activeDrawers) {
                    { paddingValues -> ExampleDrawerContent(Orientation.Vertical, "Bottom Drawer", bottomDrawerItems) }
                } else EmptyComposable,
                leftDrawerState = leftDrawerState,
                leftDrawer = if (DrawerAnchor.LEFT in activeDrawers) {
                    { paddingValues -> ExampleDrawerContent(Orientation.Horizontal, "Left Drawer", leftDrawerItems) }
                } else EmptyComposable,
                rightDrawerState = rightDrawerState,
                rightDrawer = if (DrawerAnchor.RIGHT in activeDrawers) {
                    { paddingValues -> ExampleDrawerContent(Orientation.Horizontal, "Right Drawer", rightDrawerItems) }
                } else EmptyComposable
            ) {
                // Main content

                LazyColumn(modifier = Modifier.fillMaxSize().debugBorder()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.compose_multiplatform),
                                contentDescription = "Image",
                            )
                        }
                    }
                    items((1..3).toList()) { index ->
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            text = "Main Content $index",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private data class ExampleDrawerContentItems(
    val itemCount: Int,
    val innerItemCounts: List<Int>
)

private fun randomExampleDrawerContentItems(): ExampleDrawerContentItems {
    val itemCount = Random.nextInt(from = 2, until = 23)
    val maxInnerItemCount = 7 - ((itemCount - 2) * 5 / 20)
    return ExampleDrawerContentItems(
        itemCount = itemCount,
        innerItemCounts = List(itemCount) { Random.nextInt(from = 1, until = maxInnerItemCount + 1) }
    )
}

@Composable
private fun ExampleDrawerContent(
    orientation: Orientation,
    s: String,
    items: ExampleDrawerContentItems,
    color: Color = Color.LightGray
) {
    val modifier = if (orientation == Orientation.Vertical) Modifier.fillMaxWidth() else Modifier.fillMaxHeight()
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    Box(
        modifier = modifier
            .background(color)
            .debugBorder()
            .horizontalScroll(horizontalScrollState)
            .verticalScroll(verticalScrollState),
    ) {
        Column {
                repeat(items.itemCount) { index ->
                Row {
                    Text(
                        text = "${items.itemCount}c/${items.innerItemCounts.max()}r",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(8.dp))
                    repeat(items.innerItemCounts[index]) { innerIndex ->
                        Text(
                            text = "$s ${index + 1}.${innerIndex + 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        if (innerIndex < items.innerItemCounts[index] - 1) {
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}
