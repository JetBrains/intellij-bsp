package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.openapi.diagnostic.thisLogger
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

internal class BaseBspSyncTask(
  private val project: Project,
  private val errorCallback: (Throwable) -> Unit,
  private val cancelOn: CompletableFuture<Void>,
) {

  fun execute(
    server: BspServer,
    buildServerCapabilities: BazelBuildServerCapabilities,
    buildProject: Boolean
  ): List<BaseBspTargetInfo> {
    try {
      val workspaceBuildTargetsFuture =
        if (buildProject) query1("workspace/buildAndGetBuildTargets") { server.workspaceBuildAndGetBuildTargets() }
        else query1("workspace/buildTargets") { server.workspaceBuildTargets() }
      val workspaceBuildTargets = workspaceBuildTargetsFuture?.get() ?: error("XD")

      val allTargetsIds = calculateAllTargetsIds(workspaceBuildTargets)

      val sources = query1("buildTarget/sources") {
        server.buildTargetSources(SourcesParams(allTargetsIds))
      }?.get() ?: error("XD")

      val resources = query(buildServerCapabilities.resourcesProvider == true, "buildTarget/resources") {
        server.buildTargetResources(ResourcesParams(allTargetsIds))
      }?.get() ?: error("XD")

      return workspaceBuildTargets.targets.map { targ ->
        BaseBspTargetInfo(
          target = targ,
          sources = sources.items.filter { it.target == targ.id },
          resources = resources.items.filter { it.target == targ.id }
        )
      }
    } catch (e: Exception) {
      // TODO the type xd
      if (e is ExecutionException && e.cause is CancellationException) {
        thisLogger().debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
      } else {
        thisLogger().error("calculateProjectDetailsWithCapabilities has failed", e)
      }
    }
    error("XD")
  }

  private fun calculateAllTargetsIds(
    workspaceBuildTargetsResult: WorkspaceBuildTargetsResult,
  ): List<BuildTargetIdentifier> =
    workspaceBuildTargetsResult.targets.map { it.id }

  private fun <Result> query1(
    queryName: String,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        thisLogger().warn("Query '$queryName' has failed", it)
        null
      }

  private fun <Result> query(
    check: Boolean,
    queryName: String,
    doQuery: () -> CompletableFuture<Result>,
  ): CompletableFuture<Result>? =
    if (check) doQuery()
      .reactToExceptionIn(cancelOn)
      .catchSyncErrors(errorCallback)
      .exceptionally {
        thisLogger().warn("Query '$queryName' has failed", it)
        null
      }
    else null

  private fun <T> CompletableFuture<T>.catchSyncErrors(errorCallback: (Throwable) -> Unit): CompletableFuture<T> =
    this.whenComplete { _, exception ->
      exception?.let { errorCallback(it) }
    }
}

public interface ProjectSyncHook {
  public fun isEnabled(project: Project): Boolean

  public fun execute(
    project: Project,
    server: BspServer,
    capabilities: BazelBuildServerCapabilities,
    baseInfos: List<BaseBspTargetInfo>,
    diff: AllProjectStructuresDiff,
    errorCallback: (Throwable) -> Unit,
    cancelOn: CompletableFuture<Void>,
  )

  public companion object {
    internal val ep = ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.xd")
  }
}
