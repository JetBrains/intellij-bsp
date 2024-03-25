package org.jetbrains.plugins.bsp.jvm

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesAndroid
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.*
import java.util.*
import java.util.concurrent.CompletableFuture

public class JvmBspRunHandler(private val configuration: BspRunConfigurationBase) : BspRunHandler {
  override val name: String = "Jvm BSP Run Handler"

  override val settings: BspRunConfigurationSettings
    get() = TODO("Not yet implemented")

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState {
    return when {
      executor is DefaultDebugExecutor -> {
        JvmDebugHandlerState(environment, configuration, UUID.randomUUID().toString())
      }

      configuration is BspTestConfiguration -> {
        BspTestCommandLineState(environment, configuration, UUID.randomUUID().toString())
      }

      configuration is BspRunConfiguration -> {
        BspRunCommandLineState(environment, configuration, UUID.randomUUID().toString())
      }

      else -> {
        throw ExecutionException("JvmBspRunHanlder can run only JVM or generic BSP targets")
      }
    }
  }

  public object JvmBspRunHandlerProvider : BspRunHandlerProvider {
    override val id: String = "JvmBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfigurationBase): BspRunHandler =
      JvmBspRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        it.languageIds.isJvmTarget() ||
            it.languageIds.includesAndroid() && it.capabilities.canTest
      }

  }
}

public class JvmDebugHandlerState(
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfigurationBase,
  private val originId: OriginId,
) : BspCommandLineStateBase(environment, configuration, originId) {
  public val remoteConnection: RemoteConnection =
    RemoteConnection(true, "localhost", "0", true)

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
    val targetId = BuildTargetIdentifier(configuration.targets.single())
    val runParams = RunParams(targetId)
    runParams.originId = originId
    val remoteDebugData = RemoteDebugData("jdwp", portForDebug!!)
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    return server.buildTargetRunWithDebug(runWithDebugParams) as CompletableFuture<Any>
  }
}
