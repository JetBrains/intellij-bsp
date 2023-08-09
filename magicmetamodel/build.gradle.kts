plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
  alias(libs.plugins.serialization)
}

dependencies {
  implementation(libs.bsp4kt)
  implementation(libs.jsonrpc4kt)
  implementation(libs.kotlinxJson)
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