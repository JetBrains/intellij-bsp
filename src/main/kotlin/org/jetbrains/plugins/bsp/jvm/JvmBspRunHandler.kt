package org.jetbrains.plugins.bsp.jvm

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.ui.configuration.run.BspDebugType
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import org.jetbrains.plugins.bsp.ui.configuration.run.GenericBspRunHandlerState

public class JvmBspRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean = targets.all { it.languageIds.isJvmTarget() }

  override fun canDebug(targets: List<BuildTargetInfo>): Boolean = canRun(targets)

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
  ): RunProfileState = JvmBspRunHandlerState(project, environment, configuration.targetId)

  public class JvmBspRunHandlerState(
    project: Project,
    environment: ExecutionEnvironment,
    targetId: String,
  ) : GenericBspRunHandlerState(project, environment, targetId) {
    public val remoteConnection: RemoteConnection =
      RemoteConnection(true, "localhost", "0", true)

    override val debugType: BspDebugType = BspDebugType.JDWP

    override val portForDebug: Int?
      get() = remoteConnection.debuggerAddress?.toInt()
  }
}
