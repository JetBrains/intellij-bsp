package org.jetbrains.plugins.bsp.ui.misc.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.bsp.config.BazelBspConstants
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.flow.open.BspStartupActivity
import org.jetbrains.plugins.bsp.flow.open.initProperties
import org.jetbrains.plugins.bsp.flow.open.initializeEmptyMagicMetaModel
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import org.jetbrains.plugins.bsp.services.BspCoroutineService

public val ELIGIBLE_BAZEL_PROJECT_FILE_NAMES: List<String> = listOf(
  "projectview.bazelproject",
  "WORKSPACE",
  "WORKSPACE.bazel",
)

public class OpenBazelProjectViaBspPluginAction :
  AnAction(BspPluginBundle.message("open.bazel.via.bsp.action.text")), DumbAware {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)

    val projectRootDir = calculateProjectRootDir(project, psiFile)
    performOpenBazelProjectViaBspPlugin(project, projectRootDir)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)

    val importable = isImportableBazelBspProject(project, psiFile)

    e.presentation.isEnabled = importable
    e.presentation.isVisible =
      psiFile?.virtualFile?.name?.let { ELIGIBLE_BAZEL_PROJECT_FILE_NAMES.contains(it) } == true && importable
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

public fun isImportableBazelBspProject(project: Project?, psiFile: PsiFile? = null): Boolean {
  if (project?.isBspProject == true) return false

  val projectRootDir = calculateProjectRootDir(project, psiFile) ?: return false

  val bazelBspGenerator = BspConnectionDetailsGeneratorExtension
    .extensions()
    .firstOrNull { it.id() == BazelBspConstants.ID }

  val bazelBspConnectionFile = BspConnectionFilesProvider(projectRootDir)
    .connectionFiles
    .firstOrNull { it.bspConnectionDetails?.name == BazelBspConstants.ID }

  return bazelBspGenerator?.canGenerateBspConnectionDetailsFile(projectRootDir) == true ||
      bazelBspConnectionFile != null
}

private fun calculateProjectRootDir(project: Project?, psiFile: PsiFile?): VirtualFile? =
  psiFile?.virtualFile?.parent ?: project?.guessProjectDir()

public fun performOpenBazelProjectViaBspPlugin(project: Project?, projectRootDir: VirtualFile?) {
  if (projectRootDir != null && project != null) {
    project.initProperties(projectRootDir)
    project.initializeEmptyMagicMetaModel()
    BspCoroutineService.getInstance(project).start {
      BspStartupActivity().execute(project)
    }
  }
}
