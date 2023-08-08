plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation(libs.bsp4kt)
  implementation(libs.jsonrpc4kt)
  implementation(libs.junitJupiter)
  implementation(libs.kotest)
}

intellij {
  plugins.set(Platform.plugins)
}
