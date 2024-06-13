package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

public data class BaseBspTargetInfo(
  public val target: BuildTarget,
  public val sources: List<SourcesItem>,
  public val resources: List<ResourcesItem>,
)

internal class BaseBspSyncTask {

  fun xd(server: BspServer,
         buildServerCapabilities: BazelBuildServerCapabilities, buildProject: Boolean): BaseBspTargetInfo {
    try {
      val workspaceBuildTargetsResult =
        if (!buildProject) query(true, "workspace/buildTargets") { server.workspaceBuildTargets() }!!
          .get() else query(true, "workspace/buildAndGetBuildTargets") { server.workspaceBuildAndGetBuildTargets() }!!
          .get()

      val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargetsResult)

      val sourcesFuture = query(true, "buildTarget/sources") {
        server.buildTargetSources(SourcesParams(allTargetsIds))
      }!!

      // We have to check == true because bsp4j uses non-primitive Boolean (which is Boolean? in Kotlin)
      val resourcesFuture = query(buildServerCapabilities.resourcesProvider == true, "buildTarget/resources") {
        server.buildTargetResources(ResourcesParams(allTargetsIds))
      }

      return BaseBspTargetInfo(
        targets = workspaceBuildTargetsResult.targets.toSet(),
        sources = sourcesFuture.get().items,
        resources = resourcesFuture?.get()?.items ?: emptyList(),
      )
    } catch (e: Exception) {
      // TODO the type xd
      if (e is ExecutionException && e.cause is CancellationException) {
        log.debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
      } else {
        log.error("calculateProjectDetailsWithCapabilities has failed", e)
      }
    }
  }

  private fun calculateAllTargetsIds(
    workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
  ): List<BuildTargetIdentifier> =
    workspaceBuildTargetsResult.targets.map { it.id }

  private fun <Result> query(
    check: Boolean,
    queryName: String,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    if (check) doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        log.warn("Query '$queryName' has failed", it)
        null
      }
    else null
}

public interface ProjectSyncHook {
  public fun isEnabled(project: Project): Boolean

  public fun execute(
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff
  )

  public companion object {
    internal val ep = ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.projectSyncHook")
  }
}

public class PythonSync : ProjectSyncHook {


  fun <Result> query(
    check: Boolean,
    queryName: String,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    if (check) doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        log.warn("Query '$queryName' has failed", it)
        null
      }
    else null
  private fun calculatePythonTargetsIds(
    workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
  ): List<BuildTargetIdentifier> =
    workspaceBuildTargetsResult.targets.filter { it.languageIds.includesPython() }.map { it.id }

  override fun isEnabled(project: Project): Boolean {
    TODO("Not yet implemented")
  }

  override fun execute(
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff
  ) {
    val pythonTargetsIds = baseInfos.filter { it.target.languageIds.includesPython() }.map { it.target.id }
    val pythonOptionsFuture =
      query(pythonTargetsIds.isNotEmpty() && BspFeatureFlags.isPythonSupportEnabled, "buildTarget/pythonOptions") {
        server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
      }

    val pythonOptionsFuture1 = pythonOptionsFuture!!.get()
    // TODO wodkapce model update

  }
}
