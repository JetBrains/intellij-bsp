plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
  alias(libs.plugins.ktfmt)
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}

intellij {
  plugins.set(Platform.plugins)
}

kotlin {
  sourceSets.main {
    kotlin.srcDirs("src/main/kotlin", "src/main/gen")
  }
}