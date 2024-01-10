package org.jetbrains.plugins.bsp.jewel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import org.jetbrains.jewel.bridge.LocalComponent
import org.jetbrains.jewel.bridge.toComposeColor
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.modifier.trackComponentActivation
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.jewel.ui.tree.TargetsTree
import org.jetbrains.plugins.bsp.jewel.ui.tree.TreeGenerationParams
import org.jetbrains.plugins.bsp.jewel.ui.tree.TreeTabName
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

internal class ToolWindowPanelContent(val project: Project) {
  private fun getTargetsFromMMM(): TargetsState {
    val mmmValue = MagicMetaModelService.getInstance(project).value
    return TargetsState(
      loadedTargets = mmmValue.getAllLoadedTargets(),
      invalidTargets = mmmValue.getAllInvalidTargets(),
      notLoadedTargets = mmmValue.getAllNotLoadedTargets(),
    )
  }

  private fun getTreeGenerationParams(treeTabName: TreeTabName): TreeGenerationParams {
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    val icon = if (treeTabName == TreeTabName.LOADED_TARGETS) {
      assetsExtension.loadedTargetIcon
    } else {
      assetsExtension.unloadedTargetIcon
    }
    return TreeGenerationParams(
      targetIcon = icon,
      invalidTargetIcon = assetsExtension.invalidTargetIcon,
      buildToolId = project.buildToolId,
      project = project,
      treeTabName = treeTabName,
    )
  }

  @OptIn(ExperimentalJewelApi::class)
  @Composable
  fun render() {
    val bgColor by remember(JewelTheme.isDark) { mutableStateOf(JBColor.PanelBackground.toComposeColor()) }
    Column(
      modifier = Modifier.trackComponentActivation(LocalComponent.current)
        .fillMaxSize()
        .background(bgColor),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // ------------------------- Composable toolbar --------------------------
      var panelState by remember { mutableStateOf(ToolbarTab.LOADED_TARGETS) }
      ComposableToolbar(
        panelState = panelState,
        setPanelState = { panelState = it }
      ).render()

      var mmmTargetsState by remember { mutableStateOf(getTargetsFromMMM()) }
      MagicMetaModelService.getInstance(project)
        .value
        .registerTargetLoadListener { mmmTargetsState = getTargetsFromMMM() }
      val loadedTargetsTreeParams = remember { getTreeGenerationParams(TreeTabName.LOADED_TARGETS) }
      val notLoadedTargetsTreeParams = remember { getTreeGenerationParams(TreeTabName.NOT_LOADED_TARGETS) }

      targetsTabs(panelState, mmmTargetsState, loadedTargetsTreeParams, notLoadedTargetsTreeParams)
    }
  }

  @Composable
  private fun ColumnScope.targetsTabs(
    panelState: ToolbarTab,
    targets: TargetsState,
    loadedTargetsTreeParams: TreeGenerationParams,
    notLoadedTargetsTreeParams: TreeGenerationParams,
  ) {
    when (panelState) {
      ToolbarTab.LOADED_TARGETS -> {
        loadedTargetsTab(targets.loadedTargets, targets.invalidTargets, loadedTargetsTreeParams)
      }

      ToolbarTab.NOT_LOADED_TARGETS -> {
        notLoadedTargetsTab(targets.notLoadedTargets, notLoadedTargetsTreeParams)
      }
    }
  }
}

internal enum class ToolbarTab {
  LOADED_TARGETS,
  NOT_LOADED_TARGETS,
}

@Composable
private fun ColumnScope.loadedTargetsTab(
  loadedTargets: List<BuildTargetInfo>,
  invalidTargets: List<BuildTargetId>,
  treeGenerationParams: TreeGenerationParams,
) {
  TargetsTree(
    generationParams = treeGenerationParams,
    targets = loadedTargets,
    invalidTargets = invalidTargets,
  ).render()
}

@Composable
private fun ColumnScope.notLoadedTargetsTab(
  notLoadedTargets: List<BuildTargetInfo>,
  treeGenerationParams: TreeGenerationParams,
) {
  TargetsTree(
    generationParams = treeGenerationParams,
    targets = notLoadedTargets,
    invalidTargets = emptyList(),
  ).render()
}

private data class TargetsState(
  var loadedTargets: List<BuildTargetInfo>,
  var invalidTargets: List<BuildTargetId>,
  var notLoadedTargets: List<BuildTargetInfo>,
)
