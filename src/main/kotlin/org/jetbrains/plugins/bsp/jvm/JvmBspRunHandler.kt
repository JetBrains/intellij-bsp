package org.jetbrains.plugins.bsp.jvm

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.run.*
import java.util.*
import java.util.concurrent.CompletableFuture

public class JvmBspRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean = targets.all { it.languageIds.isJvmTarget() }

  override fun canDebug(targets: List<BuildTargetInfo>): Boolean = canRun(targets)

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState {
    this.thisLogger().warn("Using generic JVM handler for ${configuration.name}")
    return JvmBspRunHandlerState(project, environment, configuration, UUID.randomUUID().toString())
  }

  public class JvmBspRunHandlerState(
    project: Project,
    environment: ExecutionEnvironment,
    private val configuration: BspRunConfigurationBase,
    private val originId: OriginId,
  ) : BspCommandLineStateBase(project, environment, configuration, originId) {
    public val remoteConnection: RemoteConnection =
      RemoteConnection(true, "localhost", "0", true)

    public val debugType: BspDebugType = BspDebugType.JDWP

    public val portForDebug: Int?
      get() = remoteConnection.debuggerAddress?.toInt()

    override fun checkRun(capabilities: BazelBuildServerCapabilities) {
      if (!capabilities.runWithDebugProvider) {
        throw ExecutionException("BSP server does not support running")
      }
    }

    override fun makeTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener =
      BspRunTaskListener(handler)

    override fun startBsp(server: BspServer): CompletableFuture<Any> {
      // SAFETY: safe to unwrap because we checked in checkRun
      val targetId = BuildTargetIdentifier(configuration.targets.single().id)
      val runParams = RunParams(targetId)
      runParams.originId = originId
      val remoteDebugData = RemoteDebugData(debugType.s, portForDebug!!)
      val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

      return server.buildTargetRunWithDebug(runWithDebugParams) as CompletableFuture<Any>
    }
  }
}
