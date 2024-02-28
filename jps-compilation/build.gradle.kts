import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("intellijbsp.kotlin-conventions")
    id("org.jetbrains.intellij.platform.base")
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, Platform.version)

        plugins(Platform.plugins)
        bundledPlugins(Platform.bundledPlugins)
        instrumentationTools()
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}