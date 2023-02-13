package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspBuildTargetClassifierProvider
import java.awt.Component
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.*

public class BuildTargetTree(
  private val targetIcon: Icon,
  private val toolName: String,
  private val targets: Collection<BuildTarget>
) : BuildTargetContainer {

  private val rootNode = DefaultMutableTreeNode(DirectoryNodeData("[root]"))
  private val cellRenderer = TargetTreeCellRenderer(targetIcon)
  private val mouseListenerBuilders = mutableSetOf<(BuildTargetContainer) -> MouseListener>()

  public val treeComponent: Tree = Tree(rootNode)

  init {
    treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    treeComponent.cellRenderer = cellRenderer
    treeComponent.isRootVisible = false
    generateTree()
    treeComponent.expandPath(TreePath(rootNode.path))
  }

  private fun generateTree() {
    val bspBuildTargetClassifierProvider =
      BspBuildTargetClassifierProvider(toolName, BspBuildTargetClassifierExtension.extensions())
    generateTreeFromIdentifiers(
      targets.map {
        BuildTargetTreeIdentifier(
          it,
          bspBuildTargetClassifierProvider.getBuildTargetPath(it),
          bspBuildTargetClassifierProvider.getBuildTargetName(it)
        )
      },
      bspBuildTargetClassifierProvider.getSeparator()
    )
  }

  private fun generateTreeFromIdentifiers(targets: List<BuildTargetTreeIdentifier>, separator: String?) {
    val pathToIdentifierMap = targets.groupBy { it.path.firstOrNull() }
    val sortedDirs = pathToIdentifierMap.keys
      .filterNotNull()
      .sorted()
    rootNode.removeAllChildren()
    for (dir in sortedDirs) {
      pathToIdentifierMap[dir]?.let {
        rootNode.add(generateDirectoryNode(it, dir, separator, 0))
      }
    }
    pathToIdentifierMap[null]?.forEach {
      rootNode.add(generateTargetNode(it))
    }
  }

  private fun generateDirectoryNode(
    targets: List<BuildTargetTreeIdentifier>,
    dirname: String,
    separator: String?,
    depth: Int
  ): DefaultMutableTreeNode {
    val node = DefaultMutableTreeNode()
    node.userObject = DirectoryNodeData(dirname)

    val pathToIdentifierMap = targets.groupBy { it.path.getOrNull(depth + 1) }
    val childrenNodeList =
      generateChildrenNodes(pathToIdentifierMap, separator, depth).toMutableList()

    simplifyNodeIfHasOneChild(node, dirname, separator, childrenNodeList)
    childrenNodeList.forEachIndexed { index, child ->
      if (child is MutableTreeNode) {
        node.insert(child, index)
      }
    }
    return node
  }

  private fun generateChildrenNodes(
    pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>,
    separator: String?,
    depth: Int
  ): List<TreeNode> {
    val childrenDirectoryNodes =
      generateChildrenDirectoryNodes(pathToIdentifierMap, separator, depth)
    val childrenTargetNodes =
      generateChildrenTargetNodes(pathToIdentifierMap)
    return childrenDirectoryNodes + childrenTargetNodes
  }

  private fun generateChildrenDirectoryNodes(
    pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>,
    separator: String?,
    depth: Int
  ): List<TreeNode> =
    pathToIdentifierMap
      .keys
      .filterNotNull()
      .sorted()
      .mapNotNull { dir ->
        pathToIdentifierMap[dir]?.let {
          generateDirectoryNode(it, dir, separator, depth + 1)
        }
      }

  private fun generateChildrenTargetNodes(
    pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>,
  ): List<TreeNode> =
    pathToIdentifierMap[null]?.map(::generateTargetNode) ?: emptyList()

  private fun generateTargetNode(identifier: BuildTargetTreeIdentifier): DefaultMutableTreeNode {
    return DefaultMutableTreeNode(TargetNodeData(identifier.target, identifier.displayName))
  }

  private fun simplifyNodeIfHasOneChild(
    node: DefaultMutableTreeNode,
    dirname: String,
    separator: String?,
    childrenList: MutableList<TreeNode>
  ) {
    if (separator != null && childrenList.size == 1) {
      val onlyChild = childrenList.first() as? DefaultMutableTreeNode
      val onlyChildObject = onlyChild?.userObject
      if (onlyChildObject is DirectoryNodeData) {
        node.userObject = DirectoryNodeData("${dirname}${separator}${onlyChildObject.name}")
        childrenList.clear()
        childrenList.addAll(onlyChild.children().toList())
      }
    }
  }

  override fun isEmpty(): Boolean = rootNode.isLeaf

  override fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener) {
    mouseListenerBuilders.add(listenerBuilder)
    treeComponent.addMouseListener(listenerBuilder(this))
  }

  override fun getSelectedBuildTarget(): BuildTarget? {
    val selected = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
    val userObject = selected?.userObject
    return if (userObject is TargetNodeData) {
      userObject.target
    } else {
      null
    }
  }

  override fun createNewWithTargets(newTargets: Collection<BuildTarget>): BuildTargetTree {
    val new = BuildTargetTree(targetIcon, toolName, newTargets)
    for (listenerBuilder in this.mouseListenerBuilders) {
      new.addMouseListener(listenerBuilder)
    }
    return new
  }
}

private interface NodeData
private data class DirectoryNodeData(val name: String) : NodeData {
  override fun toString(): String = name
}
private data class TargetNodeData(val target: BuildTarget, val displayName: String) : NodeData {
  override fun toString(): String = target.displayName ?: target.id.uri
}

private data class BuildTargetTreeIdentifier(
  val target: BuildTarget,
  val path: List<String>,
  val displayName: String
)

private class TargetTreeCellRenderer(val targetIcon: Icon) : TreeCellRenderer {
  override fun getTreeCellRendererComponent(
    tree: JTree?,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    return when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
      is DirectoryNodeData -> JBLabel(
        userObject.name,
        PlatformIcons.FOLDER_ICON,
        SwingConstants.LEFT
      )

      is TargetNodeData -> JBLabel(
        userObject.displayName,
        targetIcon,
        SwingConstants.LEFT
      )

      else -> JBLabel(
        "[not renderable]",
        PlatformIcons.ERROR_INTRODUCTION_ICON,
        SwingConstants.LEFT
      )
    }
  }
}
