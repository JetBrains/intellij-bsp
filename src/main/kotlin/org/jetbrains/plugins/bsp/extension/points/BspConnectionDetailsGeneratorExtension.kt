package org.jetbrains.plugins.bsp.extension.points

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.protocol.connection.BspConnectionDetailsGenerator

public interface BspConnectionDetailsGeneratorExtension : BspConnectionDetailsGenerator {

  public companion object {
    private val ep =
      ExtensionPointName.create<BspConnectionDetailsGeneratorExtension>("com.intellij.bspConnectionDetailsGeneratorExtension")

//    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> =
//      ep.extensionList

    public fun extensions(): List<BspConnectionDetailsGeneratorExtension> {
      return ep.extensionList
    }
  }
}

// TODO: NOT TESTED & SHOULD BE MOVED TO THE BAZEL PLUGIN
public class TemporaryBazelBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {

  override fun name(): String = "bazel"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "WORKSPACE" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile): VirtualFile {
    Runtime.getRuntime().exec(
      "cs launch org.jetbrains.bsp:bazel-bsp:2.1.0 -M org.jetbrains.bsp.bazel.install.Install",
      emptyArray(),
      projectPath.toNioPath().toFile()
    ).waitFor()

    return projectPath.findChild(".bsp")?.findChild("bazelbsp.json")!!
  }
}

public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
  override fun name(): String = "sbt"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "build.sbt" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile): VirtualFile {
    Runtime.getRuntime().exec(
      "cs launch sbt -- bspConfig",
      emptyArray(),
      projectPath.toNioPath().toFile()
    ).waitFor()

//    val console =
//      GeneralCommandLine()
//        .withWorkDirectory(projectPath.path)
//        .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.SYSTEM)
//        .withExePath("cs")
//        .withParameters(listOf("launch", "sbt", "--", "bspConfig"))
//    console.createProcess().waitFor()

    projectPath.refresh(false, false)
    val bspFolder = projectPath.findChild(".bsp")
    bspFolder?.refresh(false, true)   // only .bsp gets refreshed recursively
    return bspFolder?.findChild("sbt.json")!!
  }

}
