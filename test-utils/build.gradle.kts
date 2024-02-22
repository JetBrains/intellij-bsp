import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType


plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
}

dependencies {
  implementation(libs.bsp4j)
  implementation(libs.junitJupiter)
  implementation(libs.kotest)

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

