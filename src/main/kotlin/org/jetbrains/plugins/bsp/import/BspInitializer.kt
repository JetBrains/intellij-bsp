package org.jetbrains.plugins.bsp.import

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.ide.wizard.StepAdapter
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.observable.properties.AtomicLazyProperty
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.PlatformProjectOpenProcessor.Companion.isNewProject
import com.intellij.project.stateStore
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.enableIf
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.visibleIf
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.io.exists
import com.intellij.util.io.readText
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.ui.widgets.document.targets.BspDocumentTargetsWidget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetFactory
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.writeText

/**
 * Runs actions after the project has started up and the index is up-to-date.
 *
 * @see BspProjectOpenProcessor for additional actions that
 * may run when a project is being imported for the first time.
 */
public class BspInitializer : StartupActivity {
  override fun runActivity(project: Project) {
    val connectionService = project.getService(BspConnectionService::class.java)
    val bspUtilService = BspUtilService.getInstance()

    XdWizzard(project).showAndGet()
    val locatedBspConnectionDetails: LocatedBspConnectionDetails? =
      bspUtilService.bspConnectionDetails[project.locationHash]

    val statusBar = WindowManager.getInstance().getStatusBar(project)
    statusBar.addWidget(BspDocumentTargetsWidget(project), "before git", BspDocumentTargetsWidget(project))

    if (project.isNewProject() || locatedBspConnectionDetails == null) {
      println("BspInitializer.runActivity")

      val magicMetaModelService = MagicMetaModelService.getInstance(project)

      val task = object : Task.Backgroundable(project, "Loading changes...", true) {

        private var magicMetaModelDiff: MagicMetaModelDiff? = null

        override fun run(indicator: ProgressIndicator) {
          val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
          val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

          connectionService.connectFromDialog(project)

          val bspResolver =
            VeryTemporaryBspResolver(
              project.stateStore.projectBasePath,
              connectionService.server!!,
              bspSyncConsoleService.bspSyncConsole,
              bspBuildConsoleService.bspBuildConsole
            )

          val projectDetails = bspResolver.collectModel()

          magicMetaModelService.initializeMagicModel(projectDetails)
          val magicMetaModel = magicMetaModelService.magicMetaModel

          magicMetaModelDiff = magicMetaModel.loadDefaultTargets()
        }

        override fun onSuccess() {
          runWriteAction { magicMetaModelDiff?.applyOnWorkspaceModel() }


          ToolWindowManager.getInstance(project).registerToolWindow("BSP") {
            icon = BspPluginIcons.bsp
            canCloseContent = false
            anchor = ToolWindowAnchor.RIGHT
            contentFactory = BspAllTargetsWidgetFactory()
          }
        }
      }
      task.queue()
    }
  }
}

public class XdWizzard(project: Project) : AbstractWizard<StepAdapter>("LOLO", project) {
  init {
    val a = XdWizStep(project.guessProjectDir()!!, true)
    addStep(a)
    addStep(XdWizStep2(Path(project.basePath!!), a.connectionFileOrNewConnectionProperty))
    init()
  }


  override fun getHelpID(): String? =
    "DASDASDAdSssdss"

}

public sealed interface ConnectionFileOrNewConnection

public data class ConnectionFile(val locatedBspConnectionDetails: LocatedBspConnectionDetails) :
  ConnectionFileOrNewConnection

public object NewConnection : ConnectionFileOrNewConnection


public abstract class XdWizStepppp : StepAdapter() {


  protected abstract val panel: DialogPanel

  override fun getComponent(): JComponent = panel

  override fun _commit(finishChosen: Boolean) {
    panel.apply()
    commit(finishChosen)
  }

  protected open fun commit(finishChosen: Boolean) {}
}

public open class XdWizStep(private val projectPath: VirtualFile, private val isGeneratorAvailable: Boolean) :
  XdWizStepppp() {

  public val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection> =
    AtomicLazyProperty { calculateDefaultConnectionFileOrNewConnectionProperty(projectPath) }

  private fun calculateDefaultConnectionFileOrNewConnectionProperty(projectPath: VirtualFile): ConnectionFileOrNewConnection {
    val firstConnectionFileOrNull = calculateAvailableConnectionFiles(projectPath).firstOrNull()

    return firstConnectionFileOrNull?.let { ConnectionFile(it) } ?: NewConnection
  }

  protected override val panel: DialogPanel = panel {
    row {
      panel {
        buttonsGroup {
          calculateAvailableConnectionFiles(projectPath).map {
            row {
              radioButton(calculateConnectionFileDisplayName(it), ConnectionFile(it))
                .comment(calculateConnectionFileComment(it))
            }
          }

          row {
            radioButton(newConnectionPrompt, NewConnection)
          }.visible(isGeneratorAvailable)

        }.bind({ connectionFileOrNewConnectionProperty.get() }, { connectionFileOrNewConnectionProperty.set(it) })
      }
    }
  }

  private fun calculateAvailableConnectionFiles(projectPath: VirtualFile): List<LocatedBspConnectionDetails> =
    projectPath.findChild(".bsp")
      ?.children
      .orEmpty().toList()
      .filter { it.extension == "json" }
      .map { parseConnectionFile(it) }

  private fun parseConnectionFile(virtualFile: VirtualFile): LocatedBspConnectionDetails {
    val fileContent = virtualFile.toNioPath().readText()

    return LocatedBspConnectionDetails(
      bspConnectionDetails = Gson().fromJson(fileContent, BspConnectionDetails::class.java),
      connectionFileLocation = virtualFile,
    )
  }

  private fun calculateConnectionFileDisplayName(locatedBspConnectionDetails: LocatedBspConnectionDetails): String {
    val parentDirName = locatedBspConnectionDetails.connectionFileLocation.parent.name
    val fileName = locatedBspConnectionDetails.connectionFileLocation.name

    return "Connection file: $parentDirName/$fileName"
  }

  private fun calculateConnectionFileComment(locatedBspConnectionDetails: LocatedBspConnectionDetails): String {
    val serverName = locatedBspConnectionDetails.bspConnectionDetails.name
    val serverVersion = locatedBspConnectionDetails.bspConnectionDetails.version
    val bspVersion = locatedBspConnectionDetails.bspConnectionDetails.bspVersion
    val supportedLanguages = locatedBspConnectionDetails.bspConnectionDetails.languages.joinToString(", ")

    return """
      |Server name: $serverName$htmlBreakLine
      |Server version: $serverVersion$htmlBreakLine
      |BSP version: $bspVersion$htmlBreakLine
      |Supported languages: $supportedLanguages$htmlBreakLine
    """.trimMargin()
  }

  private companion object {
    private const val newConnectionPrompt = "New Connection"
    private const val htmlBreakLine = "<br>"
  }
}

public class XdWizStep2(
  private val projectBasePath: Path,
  private val connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>
) : XdWizStepppp() {

  private val propertyGraph = PropertyGraph(isBlockPropagation = false)

  private val projectViewFilePathProperty: GraphProperty<Path?> =
    propertyGraph
      .lazyProperty { calculateProjectViewFilePath(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateProjectViewFilePath(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateProjectViewFilePath(connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>): Path? =
    when (val connectionFileOrNewConnection = connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile ->
        calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.locatedBspConnectionDetails.bspConnectionDetails)
          ?.let { Path(it) }

      else -> projectBasePath.resolve(defaultProjectViewFileName)
    }

  private val projectViewFileNameProperty =
    propertyGraph
      .lazyProperty { calculateProjectViewFileName(projectViewFilePathProperty) }
      .also {
        it.dependsOn(projectViewFilePathProperty) {
          calculateProjectViewFileName(projectViewFilePathProperty)
        }
        projectViewFilePathProperty.dependsOn(it) {
          calculateNewProjectViewFilePath(it)
        }
      }

  private fun calculateProjectViewFileName(projectViewFilePathProperty: GraphProperty<Path?>): String =
    projectViewFilePathProperty.get()?.name ?: "Not specified"

  private fun calculateNewProjectViewFilePath(projectViewFileNameProperty: GraphProperty<String>): Path? {
    val newFileName = projectViewFileNameProperty.get()
    return projectViewFilePathProperty.get()?.parent?.resolve(newFileName)
  }

  private val isProjectViewFileNameEditableProperty =
    propertyGraph
      .lazyProperty { calculateIsProjectViewFileNameEditable(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateIsProjectViewFileNameEditable(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateIsProjectViewFileNameEditable(connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>): Boolean =
    when (connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile -> false
      else -> true
    }

  private val isProjectViewFileNameSpecifiedProperty =
    propertyGraph
      .lazyProperty { calculateIsProjectViewFileNameSpecified(connectionFileOrNewConnectionProperty) }
      .also {
        it.dependsOn(connectionFileOrNewConnectionProperty) {
          calculateIsProjectViewFileNameSpecified(connectionFileOrNewConnectionProperty)
        }
      }

  private fun calculateIsProjectViewFileNameSpecified(connectionFileOrNewConnectionProperty: ObservableMutableProperty<ConnectionFileOrNewConnection>): Boolean =
    when (val connectionFileOrNewConnection = connectionFileOrNewConnectionProperty.get()) {
      is ConnectionFile -> calculateProjectViewFileNameFromConnectionDetails(connectionFileOrNewConnection.locatedBspConnectionDetails.bspConnectionDetails) != null
      else -> true
    }

  private fun calculateProjectViewFileNameFromConnectionDetails(bspConnectionDetails: BspConnectionDetails): String? =
    bspConnectionDetails.argv.getOrNull(projectViewFileArgvIndex)

  private val projectViewTextProperty =
    propertyGraph
      .lazyProperty { calculateProjectViewText(projectViewFilePathProperty) }
      .also {
        it.dependsOn(projectViewFilePathProperty) {
          calculateProjectViewText(projectViewFilePathProperty)
        }
      }

  private fun calculateProjectViewText(projectViewFilePathProperty: GraphProperty<Path?>): String =
    projectViewFilePathProperty.get()
      ?.takeIf { it.exists() }
      ?.readText() ?: ""

  override val panel: DialogPanel = panel {
    row {
      textField()
        .label("Project view file name")
        .bindText(projectViewFileNameProperty)
        .enableIf(isProjectViewFileNameEditableProperty)
        .horizontalAlign(HorizontalAlign.FILL)
    }
    row {
      textArea()
        .bindText(projectViewTextProperty)
        .visibleIf(isProjectViewFileNameSpecifiedProperty)
        .horizontalAlign(HorizontalAlign.FILL)
        .rows(15)
    }
    row {
      text("Please choose a connection file with project view file or create a new connection in order to edit project view")
        .visibleIf(isProjectViewFileNameSpecifiedProperty.transform { !it })
    }
  }

  override fun commit(finishChosen: Boolean) {
    super.commit(finishChosen)

    if (finishChosen) {
      saveProjectViewToFileIfExist()
    }
  }

  private fun saveProjectViewToFileIfExist() =
    projectViewFilePathProperty.get()?.writeText(projectViewTextProperty.get())

  private companion object {
    private const val projectViewFileArgvIndex = 5
    private const val defaultProjectViewFileName = "projectview.bazelproject"
  }
}
