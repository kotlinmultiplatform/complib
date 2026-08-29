package com.hoffi.compose.complib

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hoffi.compose.complib.layouts.FramedContent
import complib.shared.generated.resources.Res
import complib.shared.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun AppBox() {
    val greeting = remember { Greeting().greet() }
    FramedContent(
        modifier = Modifier.fillMaxSize(),
        topPanel = {
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
        },
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
