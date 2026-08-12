package com.kibotu.ruler.sample.lib

object ClassToObfuscate {
    fun string() = "Some logic to avoid inlining (${javaClass.name})"
}
