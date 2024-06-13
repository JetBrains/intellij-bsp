package org.jetbrains.plugins.bsp.projectStructure

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.server.tasks.BspTargetInfo

public data class BuildTargetInfo(
  val target: BuildTarget,
  val moduleInfo: Module? = null,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem? = null,
  val scalacOptions: ScalacOptionsItem? = null,
  val pythonOptions: PythonOptionsItem? = null,
  val outputPathUris: List<String>,
  val libraryDependencies: List<BuildTargetIdentifier>?,
  val moduleDependencies: List<BuildTargetIdentifier>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<JvmBinaryJarsItem>,
)

public interface ProjectStructureUpdater<TDiff: ProjectStructureDiff, TInfo> {
  public val diffClass: Class<TDiff>
  public val additionalTargetInfoClass: Class<TInfo>

  public fun isSupported(buildTarget: BuildTarget): Boolean

  public fun addTarget(project: Project, targetInfo: BspTargetInfo<TInfo>, diff: TDiff)

  public companion object {
    internal val ep = ExtensionPointName.create<ProjectStructureUpdater<*, *>>("org.jetbrains.bsp.projectStructureUpdater")
  }
}
