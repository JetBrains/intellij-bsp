package org.jetbrains.plugins.bsp.jewel.ui.tree

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.LazyTree
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.assets.AssetIcon
import org.jetbrains.plugins.bsp.extension.points.BuildToolId

internal class TargetsTree(
  private val generationParams: TreeGenerationParams,
  private val targets: Collection<BuildTargetInfo>,
  private val invalidTargets: Collection<BuildTargetId>,
) {
  private companion object {
    val log = logger<TargetsTree>()
    private val nodeElementsDistance = 4.dp
  }

  private fun generateTreeStructure(): Tree<NodeData> =
    TargetsTreeStructure(generationParams.buildToolId, targets, invalidTargets).generate()

  @OptIn(ExperimentalFoundationApi::class)
  @Composable
  private fun BoxScope.renderTargetNode(data: TargetNodeData) {
    var popupMenuState by remember { mutableStateOf(PopupMenuState.HIDDEN) }
    // views the popup menu for the target
    val onRightMouseClickModifier = Modifier.onClick(
      matcher = PointerMatcher.mouse(PointerButton.Secondary)
    ) { popupMenuState = popupMenuState.toggle() }

    Row(
      horizontalArrangement = Arrangement.spacedBy(nodeElementsDistance),
      modifier = onRightMouseClickModifier,
    ) {
      // ideally icon next to the target should be the icon of the build tool, but currently in Jewel
      // rendering the icon requires a icon path which is not provided within assetsExtension.
      // Therefore, currently new `AssetIcon` class is used. See `org.jetbrains.plugins.bsp.assets.AssetIcon`
      val assetIcon = if (data.isValid) generationParams.targetIcon else generationParams.invalidTargetIcon
      val textStyle = if (data.isValid) TextStyle() else TextStyle(textDecoration = TextDecoration.LineThrough)

      Icon(resource = assetIcon.path, iconClass = assetIcon.clazz, contentDescription = null)
      Text(text = data.displayName, style = textStyle)
      TargetActionPopupMenu(
        project = generationParams.project,
        generatedTreeTabName = generationParams.treeTabName,
        state = popupMenuState,
        setState = { popupMenuState = it })
        .attach(data.target)
    }
  }

  @Composable
  private fun BoxScope.renderDirectoryNode(data: DirectoryNodeData) {
    Row(horizontalArrangement = Arrangement.spacedBy(nodeElementsDistance)) {
      Icon("nodes/folder.svg", null, AllIcons::class.java)
      Text(data.toString())
    }
  }

  @Composable
  private fun renderNode(element: Tree.Element<NodeData>) {
    val warn = { log.warn("Invalid tree element type passed during UI target tree render, rendering empty node") }
    Box(Modifier.fillMaxWidth()) {
      when (element) {
        is Tree.Element.Leaf<*> -> {
          if (element.data is TargetNodeData) renderTargetNode(element.data as TargetNodeData)
          else warn()
        }
        is Tree.Element.Node<*> -> {
          if (element.data is DirectoryNodeData) renderDirectoryNode(element.data as DirectoryNodeData)
          else warn()
        }
      }
    }
  }

  @OptIn(ExperimentalJewelApi::class)
  @Composable
  fun render() {
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      val tree = remember { generateTreeStructure() }
      val scrollState = rememberScrollState()
      LazyTree(
        tree = tree,
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        onElementClick = {},
        onElementDoubleClick = {},
        nodeContent = { renderNode(it) }
      )
    }
  }
}

internal enum class TreeTabName {
  LOADED_TARGETS,
  NOT_LOADED_TARGETS,
}

internal data class TreeGenerationParams(
  val targetIcon: AssetIcon,
  val invalidTargetIcon: AssetIcon,
  val buildToolId: BuildToolId,
  val project: Project,
  val treeTabName: TreeTabName,
)
