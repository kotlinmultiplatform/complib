package com.hoffi.compose.complib

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun printlnErr(errorMsg: String)