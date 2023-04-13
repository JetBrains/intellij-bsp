package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonLibrary
import java.net.URI
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.toPath


internal object DependencySourcesItemToPythonLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, PythonLibrary> {

  override fun transform(inputEntity: DependencySourcesItem): List<PythonLibrary> {
    return inputEntity.sources.mapNotNull { it: String ->
      val uri = URI.create(it).toPath().nameWithoutExtension
      PythonLibrary(
        "BSP: $uri",
        it
      )
    }
  }

}