package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.jetbrains.bsp.bsp4kt.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import java.nio.file.Path

internal class ResourcesItemToPythonResourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, ResourceRoot> {
  override fun transform(inputEntity: ResourcesItem): List<ResourceRoot> =
    inputEntity.resources
      .map(this::toResourceRoot)
      .filter { it.resourcePath.isPathInProjectBasePath(projectBasePath) }
      .distinct()

  private fun toResourceRoot(resourcePath: String) =
    ResourceRoot(
      resourcePath = RawUriToDirectoryPathTransformer.transform(resourcePath),
    )
}
