package org.jetbrains.plugins.bsp.server.tasks

import com.intellij.openapi.project.Project
import com.jetbrains.bsp.bsp4kt.BuildServerCapabilities
import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.bsp.bsp4kt.RunParams
import com.jetbrains.bsp.bsp4kt.RunResult
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.UUID

public class RunTargetTask(project: Project) : BspServerSingleTargetTask<RunResult>("run target", project) {

  protected override fun executeWithServer(
    server: BspServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier
  ): RunResult {
    val runParams = createRunParams(targetId)

    return server.buildTargetRun(runParams).get()
  }

  private fun createRunParams(targetId: BuildTargetIdentifier): RunParams =
    // TODO
    RunParams(targetId, originId = "run-" + UUID.randomUUID().toString(), arguments = listOf())
}
