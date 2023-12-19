package org.jetbrains.plugins.bsp.jewel.ui

import androidx.compose.runtime.Composable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.bridge.ToolWindowScope
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault

@ExperimentalCoroutinesApi
internal class BspJewelToolWindowFactory : ToolWindowFactory, DumbAware {
  override suspend fun isApplicableAsync(project: Project): Boolean = project.isBspProject
  override fun shouldBeAvailable(project: Project): Boolean =
    project.isBspProject

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.createToolWindowPanel {
      ToolWindowPanelContent(project).render()
    }
  }
}

private fun ToolWindow.createToolWindowPanel(content: @Composable ToolWindowScope.() -> Unit) {
  contentManager.removeAllContents(true)

  val panel = JewelComposePanel {
    val scope = object : ToolWindowScope {
      override val toolWindow: ToolWindow
        get() = this@createToolWindowPanel
    }
    scope.content()
  }
  contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
  this.show()
}

@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun registerBspJewelToolWindow(project: Project) {
  val toolWindowManager = ToolWindowManager.getInstance(project)
  val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
  val currentToolWindow = toolWindowManager.getToolWindow(assetsExtension.presentableName + "-Jewel")
  if (currentToolWindow == null) {
    withContext(Dispatchers.EDT) {
      toolWindowManager.registerToolWindow(assetsExtension.presentableName + "-Jewel") {
        this.icon = assetsExtension.icon
        this.anchor = ToolWindowAnchor.RIGHT
        this.canCloseContent = false
        this.contentFactory = BspJewelToolWindowFactory()
      }
    }
  }
}
