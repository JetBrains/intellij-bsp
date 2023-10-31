plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.ktfmt)
}

dependencies {
  implementation(libs.bsp4j)
}
