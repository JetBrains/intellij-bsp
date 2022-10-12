package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import org.jetbrains.plugins.bsp.services.BspUtilService
import javax.swing.Icon

public class BspProjectOpenProcessor : ProjectOpenProcessor() {

  override fun getName(): String = BspPluginBundle.message("plugin.name")

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val bspConnectionFilesProvider = BspConnectionFilesProvider(file)
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(file, BspConnectionDetailsGeneratorExtension.extensions())

    return bspConnectionFilesProvider.isAnyBspConnectionFileDefined() or
      bspConnectionDetailsGeneratorProvider.canGenerateAnyBspConnectionDetailsFile()
  }

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? {
//    val bspConnectionFilesProvider = BspConnectionFilesProvider(virtualFile)
//    val bspConnectionDetailsGeneratorProvider =
//      BspConnectionDetailsGeneratorProvider(virtualFile, BspConnectionDetailsGeneratorExtension.extensions())
    val project =
      PlatformProjectOpenProcessor.getInstance().doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame)
    project?.putUserData(BspUtilService.projectPathKey, virtualFile)

    return project
//    return if (dialog.showAndGet()) {
//      val project = PlatformProjectOpenProcessor.getInstance().doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame)
//      project?.putUserData(BspUtilService.projectPathKey, virtualFile)
//
//      val bspUtilService = BspUtilService.getInstance()
//
//      if (project != null) {
//        val connectionService = BspConnectionService.getInstance(project)
//
//        connectionService.bspConnectionDetailsGeneratorProvider = bspConnectionDetailsGeneratorProvider
//        if (dialog.buildToolUsed.selected()) {
//          connectionService.dialogBuildToolUsed = true
//          connectionService.dialogBuildToolName = dialog.buildTool
//
//          bspUtilService.selectedBuildTool[project.locationHash] = dialog.buildTool
//          bspUtilService.loadedViaBspFile.remove(project.locationHash)
//        } else {
//          bspUtilService.selectedBuildTool.remove(project.locationHash)
//          bspUtilService.loadedViaBspFile.add(project.locationHash)
//
//          connectionService.dialogBuildToolUsed = false
//          connectionService.dialogConnectionFile = bspConnectionFilesProvider.connectionFiles[dialog.connectionFileId]
//        }
//
//        project
//      } else null
//    } else null
  }
}
