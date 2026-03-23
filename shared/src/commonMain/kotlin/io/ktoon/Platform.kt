package io.ktoon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
