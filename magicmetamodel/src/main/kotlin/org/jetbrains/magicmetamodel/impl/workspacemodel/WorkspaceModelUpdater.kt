package org.jetbrains.magicmetamodel.impl.workspacemodel

import com.jetbrains.bsp.bsp4kt.BuildTarget
import com.jetbrains.bsp.bsp4kt.DependencySourcesItem
import com.jetbrains.bsp.bsp4kt.JavacOptionsItem
import com.jetbrains.bsp.bsp4kt.PythonOptionsItem
import com.jetbrains.bsp.bsp4kt.ResourcesItem
import com.jetbrains.bsp.bsp4kt.SourcesItem
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.WorkspaceModelUpdaterImpl
import java.nio.file.Path

internal data class ModuleDetails(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val pythonOptions: PythonOptionsItem?,
  val outputPathUris: List<String>,
  val libraryDependencies: List<BuildTargetId>?,
  val moduleDependencies: List<BuildTargetId>,
)

internal data class ModuleName(
  val name: String,
)

internal interface WorkspaceModelUpdater {

  fun loadModules(moduleEntities: List<Module>) =
    moduleEntities.forEach { loadModule(it) }

  fun loadModule(module: Module)

  fun loadLibraries(libraries: List<Library>)

  fun removeModules(modules: List<ModuleName>) =
    modules.forEach { removeModule(it) }

  fun removeModule(module: ModuleName)

  fun clear()

  companion object {
    fun create(
      workspaceEntityStorageBuilder: MutableEntityStorage,
      virtualFileUrlManager: VirtualFileUrlManager,
      projectBasePath: Path,
    ): WorkspaceModelUpdater =
      WorkspaceModelUpdaterImpl(
        workspaceEntityStorageBuilder = workspaceEntityStorageBuilder,
        virtualFileUrlManager = virtualFileUrlManager,
        projectBasePath = projectBasePath
      )
  }
}
