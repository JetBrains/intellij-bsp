import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.extensions.TestFrameworkType


plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.jetbrains.intellij.platform.base")
}

dependencies {
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
  implementation(libs.junitJupiter)
  implementation(libs.kotest)

  intellijPlatform {
    intellijIdeaCommunity(Platform.version)

    plugins(Platform.plugins)
    bundledPlugins(Platform.bundledPlugins)
    instrumentationTools()
    testFramework(TestFrameworkType.JUnit5)
  }
}

repositories {
  intellijPlatform {
    defaultRepositories()
  }
}
