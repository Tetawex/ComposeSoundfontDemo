package org.tetawex.cmpsftdemo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform