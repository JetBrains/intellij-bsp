package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import com.intellij.project.stateStore
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.import.wizzard.ConnectionFile
import org.jetbrains.plugins.bsp.import.wizzard.ImportProjectWizzard
import org.jetbrains.plugins.bsp.import.wizzard.NewConnection
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.document.targets.BspDocumentTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetFactory

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspInitializer : StartupActivity {
  override fun runActivity(project: Project) {
    val connectionService = project.getService(BspConnectionService::class.java)
    val bspUtilService = BspUtilService.getInstance()

    val projectPath = project.getUserData(BspUtilService.projectPathKey)!!
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(projectPath, BspConnectionDetailsGeneratorExtension.extensions())

    val locatedBspConnectionDetails: LocatedBspConnectionDetails? =
      bspUtilService.bspConnectionDetails[project.locationHash]

    val statusBar = WindowManager.getInstance().getStatusBar(project)
    statusBar.addWidget(BspDocumentTargetsWidget(project), "before git", BspDocumentTargetsWidget(project))

    if (project.isNewProject() || locatedBspConnectionDetails == null) {
      println("BspInitializer.runActivity")

      val wizzard = ImportProjectWizzard(project, bspConnectionDetailsGeneratorProvider)
      if (wizzard.showAndGet()) {

        connectionService.bspConnectionDetailsGeneratorProvider = bspConnectionDetailsGeneratorProvider
        if (wizzard.connectionFileOrNewConnectionProperty.get() is NewConnection) {
          connectionService.dialogBuildToolUsed = true
          connectionService.dialogBuildToolName = bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY()

          bspUtilService.selectedBuildTool[project.locationHash] = bspConnectionDetailsGeneratorProvider.firstGeneratorTEMPORARY() ?: ""
          bspUtilService.loadedViaBspFile.remove(project.locationHash)
        } else {
          bspUtilService.selectedBuildTool.remove(project.locationHash)
          bspUtilService.loadedViaBspFile.add(project.locationHash)

          connectionService.dialogBuildToolUsed = false
          connectionService.dialogConnectionFile = (wizzard.connectionFileOrNewConnectionProperty.get() as ConnectionFile).locatedBspConnectionDetails
        }
      }

      val magicMetaModelService = MagicMetaModelService.getInstance(project)

      val task = object : Task.Backgroundable(project, "Loading changes...", true) {

        private var magicMetaModelDiff: MagicMetaModelDiff? = null

        override fun run(indicator: ProgressIndicator) {
          val bspConsoleService = BspConsoleService.getInstance(project)

          connectionService.connectFromDialog(project)

          val bspResolver =
            VeryTemporaryBspResolver(
              project.stateStore.projectBasePath,
              connectionService.server!!,
              bspConsoleService.bspSyncConsole,
              bspConsoleService.bspBuildConsole
            )

          val projectDetails = bspResolver.collectModel()

          magicMetaModelService.initializeMagicModel(projectDetails)
          val magicMetaModel = magicMetaModelService.magicMetaModel

          magicMetaModelDiff = magicMetaModel.loadDefaultTargets()
        }

        override fun onSuccess() {
          runWriteAction { magicMetaModelDiff?.applyOnWorkspaceModel() }


          ToolWindowManager.getInstance(project).registerToolWindow("BSP") {
            icon = BspPluginIcons.bsp
            canCloseContent = false
            anchor = ToolWindowAnchor.RIGHT
            contentFactory = BspAllTargetsWidgetFactory()
          }
        }
      }
      task.queue()
    }
  }
}
