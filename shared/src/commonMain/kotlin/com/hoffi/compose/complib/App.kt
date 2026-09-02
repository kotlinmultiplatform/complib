package com.hoffi.compose.complib

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.hoffi.compose.complib.layouts.FramedContentAppBox

@Composable
@Preview
fun App() {
    MaterialTheme {
        FramedContentAppBox()
    }
}
