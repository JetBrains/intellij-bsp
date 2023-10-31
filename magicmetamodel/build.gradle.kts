plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
  alias(libs.plugins.ktfmt)
}

dependencies {
  implementation(project(":protocol"))
  implementation(libs.bsp4j)
  implementation(project(":workspacemodel"))
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}

intellij {
  plugins.set(Platform.plugins)
}