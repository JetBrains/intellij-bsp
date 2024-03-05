import org.jetbrains.intellij.platform.gradle.extensions.TestFrameworkType

plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.jetbrains.intellij.platform.base")
}

dependencies {
  implementation(project(":jps-compilation"))
  implementation(project(":protocol"))
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
  implementation(project(":workspacemodel"))
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  intellijPlatform {
    intellijIdeaCommunity(Platform.version)

    plugins(Platform.plugins)
    bundledPlugins(Platform.bundledPlugins)
    instrumentationTools()
    testFramework(TestFrameworkType.Common)
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
