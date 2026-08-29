package com.hoffi.compose.complib

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice
import platform.posix.fputs
import platform.posix.stderr

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalForeignApi::class)
actual fun printlnErr(errorMsg: String) {
    fputs(errorMsg + "\n", stderr)
}