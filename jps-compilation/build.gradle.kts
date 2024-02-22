import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("intellijbsp.kotlin-conventions")
    alias(libs.plugins.intellij)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, Platform.version)

        plugins(Platform.plugins)
        bundledPlugins(Platform.bundledPlugins)
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}