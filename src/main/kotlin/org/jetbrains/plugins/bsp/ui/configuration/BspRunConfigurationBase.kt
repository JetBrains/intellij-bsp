package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationEditor
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationSettings
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandlerProvider

public abstract class BspRunConfigurationBase(
  private val project: Project,
  configurationFactory: BspRunConfigurationTypeBase,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  DumbAware {

  private val logger: Logger = logger<BspRunConfigurationBase>()

  /** The BSP-specific parts of the last serialized state of this run configuration. */
  private var bspElementState = Element(BSP_STATE_TAG)

  public var targets: List<String> = emptyList()
    set(value) {
      handlerProvider = BspRunHandlerProvider.getRunHandlerProvider(project, value)
      field = value
    }
  private var handlerProvider: BspRunHandlerProvider = BspRunHandlerProvider.getRunHandlerProvider(project, targets)
    set(value) {
      updateHandler(value)
      field = value
    }

  public var handler: BspRunHandler = handlerProvider.createRunHandler(this)

  public var settingsEditor: SettingsEditor<BspRunConfigurationSettings> = handler.settings.getEditor(project)

  public fun interface HandlerChangeListener {
    public fun run(newHandler: BspRunHandler)
  }

  public val handlerChangeListeners: MutableList<HandlerChangeListener> = mutableListOf()

  private fun updateHandler(newProvider: BspRunHandlerProvider) {
    if (newProvider == handlerProvider) return
    try {
      handler.settings.writeExternal(bspElementState)
    } catch (e: WriteExternalException) {
      logger.error("Failed to write BSP state", e)
    }
    handler = newProvider.createRunHandler(this)
    try {
      for (listener in handlerChangeListeners) {
        listener.run(handler)
      }
    } catch (e: Exception) {
      logger.error("Error while execution handlerChangeListener", e)
    }
    try {
      handler.settings.readExternal(bspElementState)
    } catch (e: Exception) {
      logger.error("Failed to read BSP state", e)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out BspRunConfigurationBase> {
    return BspRunConfigurationEditor(this)
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return handler.getRunProfileState(executor, environment)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)

    val targets = mutableListOf<String>()
    for (targetElement in bspElementState.getChildren(TARGET_TAG)) {
      targets.add(targetElement.text)
    }

    // It should be possible to load the configuration before the project is synchronized,
    // so we can't access targets' data here. Instead, we have to use the stored provider ID.
    // TODO: is that true?
    val providerId = bspElementState.getAttributeValue(HANDLER_PROVIDER_ATTR)
    val provider = BspRunHandlerProvider.findRunHandlerProvider(providerId)
    if (provider != null) {
      handlerProvider = provider
    } else {
      logger.error("Failed to find run handler provider with ID $providerId")
    }

    bspElementState = element
    handler.settings.readExternal(bspElementState)

  }

  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    bspElementState.removeChildren(TARGET_TAG)

    for (target in targets) {
      val targetElement = Element(TARGET_TAG)
      targetElement.text = target
      bspElementState.addContent(targetElement)
    }

    bspElementState.setAttribute(HANDLER_PROVIDER_ATTR, handlerProvider.id)

    handler.settings.writeExternal(bspElementState)

    logger.warn(bspElementState.toString())
    logger.warn(element.toString())

    element.addContent(bspElementState)
  }

  public companion object {
    private const val TARGET_TAG = "bsp-target"
    private const val BSP_STATE_TAG = "bsp-state"
    private const val HANDLER_PROVIDER_ATTR = "handler-provider-id"
  }
}

public class BspRunConfiguration(
  project: Project,
  configurationFactory: BspRunConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name)

public class BspTestConfiguration(
  project: Project,
  configurationFactory: BspTestConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name),
  SMRunnerConsolePropertiesProvider {
  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
    return SMTRunnerConsoleProperties(this, "BSP", executor)
  }
}

public class BspBuildConfiguration(
  project: Project,
  configurationFactory: BspBuildConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name)
