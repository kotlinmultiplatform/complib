package com.hoffi.compose.complib

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun printlnErr(errorMsg: String) {
    System.err.println(errorMsg)
}