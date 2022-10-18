package org.jetbrains.plugins.bsp.import

import ch.epfl.scala.bsp4j.*
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.task.*
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole

/**
 * WARNING: temporary solution, might change
 */
public class BspHackProjectTaskRunner : ProjectTaskRunner() {

  override fun canRun(projectTask: ProjectTask): Boolean {
    return true
  }

  override fun run(project: Project,
                   projectTaskContext: ProjectTaskContext,
                   vararg tasks: ProjectTask): Promise<Result> {
    return buildAllBspTargets(project)
  }

  private fun buildAllBspTargets(project: Project): Promise<Result> {
    val magicMetaModelService = MagicMetaModelService.getInstance(project)
    val bspConnectionService = BspConnectionService.getInstance(project)
    val bspConsoleService = BspConsoleService.getInstance(project)

    val magicMetaModel: MagicMetaModel = magicMetaModelService.magicMetaModel
    val targets: List<BuildTarget> = magicMetaModel.getAllLoadedTargets() + magicMetaModel.getAllNotLoadedTargets()

    val bspBuildConsole: TaskConsole = bspConsoleService.bspBuildConsole

    val promiseResult = AsyncPromise<Result>()

    val bspResolver = VeryTemporaryBspResolver(
      project.stateStore.projectBasePath,
      bspConnectionService.server!!,
      bspConsoleService.bspSyncConsole,
      bspBuildConsole
    )

    bspResolver.buildTargets(
      targets
        .filter { it.capabilities.canCompile }
        .map { it.id }
    )

    return promiseResult
  }
}