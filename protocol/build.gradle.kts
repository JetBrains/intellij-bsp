plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.jetbrains.intellij.platform.base")
}

dependencies {
  implementation(libs.bsp4j)
  intellijPlatform {
    intellijIdeaCommunity(Platform.version)

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
