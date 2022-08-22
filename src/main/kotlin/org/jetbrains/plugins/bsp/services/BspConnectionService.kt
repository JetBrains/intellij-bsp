package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.BspConnectionDetails
import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildClientCapabilities
import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.ui.console.BspSyncConsole
import org.jetbrains.plugins.bsp.ui.console.ConsoleOutputStream
import org.jetbrains.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.protocol.connection.LocatedBspConnectionDetailsParser
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.jetbrains.plugins.bsp.ui.console.BspBuildConsole

public interface BspServer : BuildServer

public fun interface Cancelable {
  public fun cancel()
}
public class BspConnectionService(private val project: Project) {

  public var server: BspServer? = null
  private var bspProcess: Process? = null
  private var cancelActions: List<Cancelable> ? = null

  // consider enclosing nicely in an object
  public var dialogBuildToolUsed: Boolean? = null
  public var dialogBuildToolName: String? = null
  public var dialogConnectionFile: LocatedBspConnectionDetails? = null
  public var bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider? = null

  public fun connect(connectionFile: LocatedBspConnectionDetails) {
    val process = createAndStartProcess(connectionFile.bspConnectionDetails)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
    val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

    val client = BspClient(bspSyncConsoleService.bspSyncConsole, bspBuildConsoleService.bspBuildConsole)

    val bspIn = process.inputStream
    val bspOut = process.outputStream
    val launcher = createLauncher(bspIn, bspOut, client)

    val listening = launcher.startListening()
    bspProcess = process
    server = launcher.remoteProxy
    client.onConnectWithServer(server)
    cancelActions = listOf(
      Cancelable {
        process.destroy()
        process.waitFor(15, TimeUnit.SECONDS)
      },
      Cancelable { bspIn.close() },
      Cancelable { bspOut.close() },
      Cancelable { listening.cancel(true) },
      Cancelable { process.destroy() }
    )
  }

  public fun connectFromDialog(project: Project) {
    val bspUtilService = BspUtilService.getInstance()
    val bspSyncConsole: BspSyncConsole = BspSyncConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.startImport("bsp-obtain-config", "BSP: Obtain config", "Obtaining...")
    if (dialogBuildToolUsed!!) {
      val xd1 = bspConnectionDetailsGeneratorProvider!!.generateBspConnectionDetailFileForGeneratorWithName(
        dialogBuildToolName!!,
        ConsoleOutputStream("bsp-obtain-config", bspSyncConsole)
      )
      val xd2 = LocatedBspConnectionDetailsParser.parseFromFile(xd1!!)
      bspUtilService.bspConnectionDetails[project.locationHash] = xd2!!
      connect(xd2)
    } else {
      bspUtilService.bspConnectionDetails[project.locationHash] = dialogConnectionFile!!
      connect(dialogConnectionFile!!)
    }
    bspSyncConsole.finishImport("Config obtained!", SuccessResultImpl())
  }

  public fun reconnect(locationHash: String) {
    val bspService = BspUtilService.getInstance()
    bspService.bspConnectionDetails[locationHash]?.let {
      connect(it)
    }
  }

  public fun isRunning(): Boolean = bspProcess?.isAlive == true

  private fun createAndStartProcess(bspConnectionDetails: BspConnectionDetails): Process =
    ProcessBuilder(bspConnectionDetails.argv)
      .directory(project.stateStore.projectBasePath.toFile())
      .start()

  private fun createLauncher(bspIn: InputStream, bspOut: OutputStream, client: BuildClient): Launcher<BspServer> =
    Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(AppExecutorUtil.getAppExecutorService())
      .setInput(bspIn)
      .setOutput(bspOut)
      .setLocalService(client)
      .create()

  public fun disconnect() {
    val errors = mutableListOf<Throwable>()
    cancelActions?.forEach {
      try {
        it.cancel()
      } catch (e: Exception) {
        errors.add(e)
      }
    }
    val head = errors.firstOrNull()
    head?.let {
      errors.drop(1).forEach { head.addSuppressed(it) }
      throw head
    }
  }

  public companion object {
    public fun getInstance(project: Project): BspConnectionService =
      project.getService(BspConnectionService::class.java)
  }
}

public class VeryTemporaryBspResolver(
  private val projectBaseDir: Path,
  private val server: BspServer,
  private val bspSyncConsole: BspSyncConsole,
  private val bspBuildConsole: BspBuildConsole
) {

  public fun buildTarget(targetId: BuildTargetIdentifier) {
    val uuid = "build-" + UUID.randomUUID().toString()
    bspBuildConsole.startBuild(uuid, "BSP: Build", "Building " + targetId.uri)

    println("buildTargetCompile")
    val compileParams = CompileParams(listOf(targetId)).apply { originId = uuid }
    val compileResult = server.buildTargetCompile(compileParams).get()

    when (compileResult.statusCode) {
      StatusCode.OK -> bspBuildConsole.finishBuild("Build is successfully done!", uuid)
      StatusCode.CANCELLED -> bspBuildConsole.finishBuild("Build is cancelled!", uuid)
      StatusCode.ERROR -> bspBuildConsole.finishBuild("Build ended with an error!", uuid)
      else -> bspBuildConsole.finishBuild("Build is finished!", uuid)
    }
  }

  public fun collectModel(): ProjectDetails {
    bspSyncConsole.startImport("bsp-import", "BSP: Import", "Importing...")

    println("buildInitialize")
    server.buildInitialize(createInitializeBuildParams()).catchSyncErrors().get()

    println("onBuildInitialized")
    server.onBuildInitialized()

    println("workspaceBuildTargets")
    val workspaceBuildTargetsResult = server.workspaceBuildTargets().catchSyncErrors().get()
    val allTargetsIds = workspaceBuildTargetsResult!!.targets.map(BuildTarget::getId)

    println("buildTargetSources")
    val sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).catchSyncErrors().get()

    println("buildTargetResources")
    val resourcesResult = server.buildTargetResources(ResourcesParams(allTargetsIds)).catchSyncErrors().get()

    println("buildTargetDependencySources")
    val dependencySourcesResult = server.buildTargetDependencySources(DependencySourcesParams(allTargetsIds)).catchSyncErrors().get()

    bspSyncConsole.finishImport("Import done!", SuccessResultImpl())

    println("done done!")
    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesResult.items,
      resources = resourcesResult.items,
      dependenciesSources = dependencySourcesResult.items,
    )
  }

  private fun createInitializeBuildParams(): InitializeBuildParams {
    val params = InitializeBuildParams(
      "IntelliJ-BSP",
      "1.0.0",
      "2.0.0",
      projectBaseDir.toString(),
      BuildClientCapabilities(listOf("java"))
    )
    val dataJson = JsonObject()
    dataJson.addProperty("clientClassesRootDir", "$projectBaseDir/out")
    dataJson.add("supportedScalaVersions", JsonArray())
    params.data = dataJson

    return params
  }

  private fun <T> CompletableFuture<T>.catchSyncErrors(): CompletableFuture<T> {
    return this
      .whenComplete { _, exception ->
        exception?.let {
          bspSyncConsole.addMessage("bsp-import", "Sync failed")
          bspSyncConsole.finishImport( "Sync failed", FailureResultImpl(exception))
        }
      }
  }
}

private class BspClient(private val bspSyncConsole: BspSyncConsole, private val bspBuildConsole: BspBuildConsole) : BuildClient {

  override fun onBuildShowMessage(params: ShowMessageParams) {
    println("onBuildShowMessage")
    println(params)
    addMessageToConsole(params.task?.id, params.message, params.originId)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    println("onBuildLogMessage")
    println(params)
    addMessageToConsole(params.task?.id, params.message, params.originId)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    println("onBuildTaskStart")
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println("onBuildTaskProgress")
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    println("onBuildTaskFinish")
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams?) {
    println("onBuildPublishDiagnostics")
    println(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println("onBuildTargetDidChange")
    println(params)
  }

  private fun addMessageToConsole(id: Any?, message: String, originId: String?){
    if(originId?.startsWith("build") == true) {
      bspBuildConsole.addMessage(id, message, originId)
    } else {
      bspSyncConsole.addMessage(id, message)
    }
  }
}
