package com.hoffi.compose.complib.layouts.framedcontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hoffi.compose.complib.Greeting
import com.hoffi.compose.complib.debugBorder
import com.hoffi.compose.complib.debugMode
import complib.shared.generated.resources.Res
import complib.shared.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

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
    var previousFraming by remember { mutableStateOf(FramingStyle.STRETCH_TOP_AND_BOTTOM) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            val isReverseClick = event.keyboardModifiers.isMetaPressed || event.keyboardModifiers.isCtrlPressed
                            if (isReverseClick) {
                                val current = currentFraming
                                currentFraming = previousFraming
                                previousFraming = current
                            } else {
                                previousFraming = currentFraming
                                val currentIndex = framingStyles.indexOf(currentFraming)
                                val nextIndex = if (currentIndex == framingStyles.lastIndex) 0 else currentIndex + 1
                                currentFraming = framingStyles[nextIndex]
                            }
                        }
                    }
                }
            },
            onClick = {},
        ) {
            Text(currentFraming.name)
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
            },
            mainContent = {
                Box(
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center).debugBorder(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text(
                        modifier = Modifier.padding(10.dp),
                        text = "Main Content",
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}
