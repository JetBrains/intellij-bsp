package org.jetbrains.plugins.bsp.jewel.ui.tree

import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo

internal interface NodeData

internal data class DirectoryNodeData(val name: String) : NodeData {
  override fun toString(): String = name
}

internal data class TargetNodeData(
  val id: BuildTargetId,
  val target: BuildTargetInfo?,
  val displayName: String,
  val isValid: Boolean,
) : NodeData

internal data class TargetNodeDataWithPath(
  val nodeData: TargetNodeData,
  val path: List<String>,
)
