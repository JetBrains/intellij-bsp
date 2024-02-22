import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
  alias(libs.plugins.intellijMigration)
}

dependencies {
  implementation(project(":jps-compilation"))
  implementation(project(":protocol"))
  implementation(libs.bsp4j)
  implementation(project(":workspacemodel"))
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}
