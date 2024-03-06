rootProject.name = "intellij-bsp"
include("magicmetamodel", "protocol", "workspacemodel", "jps-compilation")

pluginManagement {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
  }
}
