import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.jetbrains.intellij.platform.base")
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
    instrumentationTools()
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
