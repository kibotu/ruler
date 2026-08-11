package com.spotify.ruler.sample.lib

object ClassToObfuscate {
    fun string() = "Some logic to avoid inlining (${javaClass.name})"
}
