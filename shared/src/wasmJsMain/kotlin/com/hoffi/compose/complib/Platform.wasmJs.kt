package com.hoffi.compose.complib

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

@OptIn(ExperimentalWasmJsInterop::class)
actual fun printlnErr(errorMsg: String) {
    js("console.error(errorMsg)")
}
