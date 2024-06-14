package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.concurrent.CompletableFuture

internal class NewBigSync(val project: Project) {
  private var diff: AllProjectStructuresDiff? = null
  fun xd(server: BspServer, buildServerCapabilities: BazelBuildServerCapabilities, buildProject: Boolean) {
    println("AAAa")
    val baseSync = BaseBspSyncTask(project, {}, CompletableFuture())
    val baseInfo = baseSync.execute(server,buildServerCapabilities, buildProject )

    // name providers
    val hooks = ProjectSyncHook.ep.extensionList.filter { it.isEnabled(project) }

    println("CCCC")
    val provider = AllProjectStructuresProvider(project)
    diff = provider.newDiff()
    hooks.forEach { it.execute(project, server, buildServerCapabilities, baseInfo, diff!!, {}, CompletableFuture()) }
    println("DDDD")
  }

  internal suspend fun a() {
    diff!!.applyAll()
  }
}
