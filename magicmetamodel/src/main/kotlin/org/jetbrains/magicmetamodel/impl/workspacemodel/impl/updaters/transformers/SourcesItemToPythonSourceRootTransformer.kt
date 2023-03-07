package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.util.io.isAncestor
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.SourceRoot

internal object SourcesItemToPythonSourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, PythonSourceRoot> {

  private const val sourceRootType = "python-source"
  private const val testSourceRootType = "python-test"

  override fun transform(inputEntities: List<BuildTargetAndSourceItem>): List<PythonSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)

    return allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRoots) }
  }

  private fun isNotAChildOfAnySourceDir(sourceRoot: PythonSourceRoot, allSourceRoots: List<PythonSourceRoot>): Boolean =
    allSourceRoots.none { it.sourcePath.isAncestor(sourceRoot.sourcePath.parent) }

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<PythonSourceRoot> {
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sourcesItem.sources)
      .map { toPythonSourceRoot(it, rootType, inputEntity.buildTarget.id) }
  }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun toPythonSourceRoot(
    sourceRoot: SourceRoot,
    rootType: String,
    targetId: BuildTargetIdentifier
  ): PythonSourceRoot {
    return PythonSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      rootType = rootType,
      targetId = targetId,
    )
  }
}
