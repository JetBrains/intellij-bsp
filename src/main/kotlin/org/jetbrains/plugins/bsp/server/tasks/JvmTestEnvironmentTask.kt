package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspServer

public class JvmTestEnvironmentTask(project: Project)
  : BspServerSingleTargetTask<JvmTestEnvironmentResult>("jvmTestEnvironment", project) {

  override fun executeWithServer(server: BspServer, targetId: BuildTargetIdentifier): JvmTestEnvironmentResult {
    val params = createJvmTestEnvironmentParams(targetId)
    return server.jvmTestEnvironment(params).get()
  }

  private fun createJvmTestEnvironmentParams(targetId: BuildTargetIdentifier) =
          JvmTestEnvironmentParams(
                  listOf(targetId),
          )
}
