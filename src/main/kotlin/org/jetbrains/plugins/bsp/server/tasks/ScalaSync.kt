//package org.jetbrains.plugins.bsp.server.tasks
//
//import ch.epfl.scala.bsp4j.PythonOptionsItem
//import ch.epfl.scala.bsp4j.ScalacOptionsItem
//import ch.epfl.scala.bsp4j.ScalacOptionsParams
//import com.intellij.openapi.project.Project
//import com.intellij.platform.workspace.jps.entities.DependencyScope
//import com.intellij.platform.workspace.jps.entities.ModuleDependency
//import com.intellij.platform.workspace.jps.entities.ModuleEntity
//import com.intellij.platform.workspace.jps.entities.ModuleId
//import com.intellij.platform.workspace.storage.MutableEntityStorage
//import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
//import org.jetbrains.plugins.bsp.config.BspFeatureFlags
//import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
//import org.jetbrains.plugins.bsp.server.connection.BspServer
//import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
//import org.jetbrains.workspacemodel.entities.BspEntitySource
//import java.util.concurrent.CompletableFuture
//
//internal class ScalaSync : ProjectSyncHook {
//  override fun isEnabled(project: Project): Boolean {
//    TODO("Not yet implemented")
//  }
//  fun <Result> query(
//    check: Boolean,
//    queryName: String,
//    doQuery: () -> CompletableFuture<Result>,
//  ): CompletableFuture<Result>? =
//    if (check) doQuery()
//      .reactToExceptionIn(cancelOn)
//      .catchSyncErrors(errorCallback)
//      .exceptionally {
//        log.warn("Query '$queryName' has failed", it)
//        null
//      }
//    else null
//
//  override fun execute(
//    server: BspServer,
//    capabilities: BazelBuildServerCapabilities,
//    baseInfos: List<BaseBspTargetInfo>,
//    diff: AllProjectStructuresDiff
//  ) {
//    val scalaTargetIds = baseInfos.map { it.target.id }
//    val scalacOptionsFuture =
//      query(scalaTargetIds.isNotEmpty() && BspFeatureFlags.isScalaSupportEnabled, "buildTarget/scalacOptions") {
//        server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
//      } // TODO libraries
//
//    val scalaxd = scalacOptionsFuture!!.get()
//
//  }
//
//  private fun MutableEntityStorage.add(baseInfos: List<BaseBspTargetInfo>, scala: List<ScalacOptionsItem>) {
//    baseInfos.forEach { baseInfo ->
//      val pyth = scala.filter { it.target == baseInfo.target }
//      add1(baseInfo, pyth)
//    }
//  }
//
//  private fun MutableEntityStorage.add1(baseInfo: BaseBspTargetInfo, scalac: List<ScalacOptionsItem>) {
//    val deps = baseInfo.target.dependencies.map {
//      ModuleDependency(
//        module = ModuleId(it.uri),
//        exported = false,
//        scope = DependencyScope.COMPILE,
//        productionOnTest = true,
//      )
//    }
//    val module = addEntity(
//      ModuleEntity(
//        name = baseInfo.target.id.uri, // TODO name mod
//        dependencies = deps,
//        entitySource = BspEntitySource // TODO,
//      ) {
//        this.type = "JAVA_MODULE"
//      }
//    )
//}