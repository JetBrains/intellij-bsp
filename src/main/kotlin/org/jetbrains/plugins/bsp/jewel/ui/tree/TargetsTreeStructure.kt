package org.jetbrains.plugins.bsp.jewel.ui.tree

import org.jetbrains.jewel.foundation.lazy.tree.ChildrenGeneratorScope
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.TreeBuilder
import org.jetbrains.jewel.foundation.lazy.tree.buildTree
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault

private typealias TreeNode = TreeBuilder.Element<NodeData>
private typealias DirectoryTreeNode = TreeBuilder.Element.Node<NodeData>
private typealias LeafTreeNode = TreeBuilder.Element.Leaf<NodeData>
private typealias ChildrenGenerator = ChildrenGeneratorScope<NodeData>.() -> Unit

internal class TargetsTreeStructure(
  private val buildToolId: BuildToolId,
  private val targets: Collection<BuildTargetInfo>,
  private val invalidTargets: Collection<BuildTargetId>,
) {
  private var separator: String? = null

  fun generate(): Tree<NodeData> {
    val bspBuildTargetClassifier = BuildTargetClassifierExtension.ep.withBuildToolIdOrDefault(buildToolId)
    separator = bspBuildTargetClassifier.separator

    val targetsCombined = targets.map {
      TargetNodeDataWithPath(
        nodeData = TargetNodeData(it.id, it, bspBuildTargetClassifier.calculateBuildTargetName(it.id), true),
        path = bspBuildTargetClassifier.calculateBuildTargetPath(it.id),
      )
    } + invalidTargets.map {
      TargetNodeDataWithPath(
        nodeData = TargetNodeData(it, null, bspBuildTargetClassifier.calculateBuildTargetName(it), false),
        bspBuildTargetClassifier.calculateBuildTargetPath(it),
      )
    }
    return treeStructureFromIdentifiers(targetsCombined)
  }

  private fun treeStructureFromIdentifiers(
    targetsNodes: List<TargetNodeDataWithPath>,
  ): Tree<NodeData> {
    val pathToIdentifierMap = targetsNodes.groupBy { it.path.firstOrNull() }
    val sortedDirs = pathToIdentifierMap.keys
      .filterNotNull()
      .sorted()

    return buildTree {
      for (dir in sortedDirs) {
        pathToIdentifierMap[dir]?.let {
          val directoryNode = generateDirectoryNode(it, dir, 0)
          add(directoryNode)
        }
      }
      pathToIdentifierMap[null]?.forEach { addLeaf(it.nodeData) }
    }
  }

  private fun generateDirectoryNode(
    targets: List<TargetNodeDataWithPath>,
    dirname: String,
    depth: Int,
  ): TreeBuilder.Element.Node<NodeData> {
    val pathToIdentifierMap = targets.groupBy { it.path.getOrNull(depth + 1) }
    val childrenNodes: List<TreeBuilder.Element<NodeData>> = generateChildrenNodes(pathToIdentifierMap, depth)
    val (nodeName, childrenGenerator) = simplifyNodeParametersIfOnlyOneChild(dirname, childrenNodes)

    return TreeBuilder.Element.Node(
      data = DirectoryNodeData(nodeName),
      id = null,
      childrenGenerator = childrenGenerator)
  }

  private fun childrenGenerator(childrenList: List<TreeNode>): ChildrenGenerator {
    return {
      childrenList.forEach { add(it) }
    }
  }

  private fun simplifyNodeParametersIfOnlyOneChild(
    dirname: String,
    childrenList: List<TreeNode>,
  ): Pair<String, ChildrenGenerator> {
    if (separator != null && childrenList.size == 1) {
      val onlyChild = childrenList.first() as? DirectoryTreeNode
      if (onlyChild != null) {
        val newName = "${dirname}${separator}${onlyChild.data}"
        return Pair(newName, onlyChild.childrenGenerator)
      }
    }
    return Pair(dirname, childrenGenerator(childrenList))
  }

  private fun generateChildrenNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeDataWithPath>>,
    depth: Int,
  ): List<TreeNode> {
    val childrenDirectoryNodes =
      generateChildrenDirectoryNodes(pathToIdentifierMap, depth)
    val childrenTargetNodes =
      generateChildrenTargetNodes(pathToIdentifierMap)
    return childrenDirectoryNodes + childrenTargetNodes
  }

  private fun generateChildrenDirectoryNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeDataWithPath>>,
    depth: Int,
  ): List<TreeNode> =
    pathToIdentifierMap
      .keys
      .filterNotNull()
      .sorted()
      .mapNotNull { dir ->
        pathToIdentifierMap[dir]?.let {
          generateDirectoryNode(it, dir, depth + 1)
        }
      }

  private fun generateChildrenTargetNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeDataWithPath>>,
  ): List<TreeNode> =
    pathToIdentifierMap[null]?.map { LeafTreeNode(it.nodeData, null) } ?: emptyList()
}
