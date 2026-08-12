plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.pluginPublish) apply false
}

allprojects {
    group = "net.kibotu"
    version = "3.0.0"
}
