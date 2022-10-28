package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import com.intellij.util.concurrency.AppExecutorUtil
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetails
import org.jetbrains.plugins.bsp.protocol.connection.LocatedBspConnectionDetailsParser
import org.jetbrains.plugins.bsp.ui.console.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

public interface BspServer : BuildServer, JavaBuildServer

private fun syncId(): String = "bsp-import"  // TODO - temporary

public fun interface Cancelable {
  public fun cancel()
}

public class BspConnectionService(private val project: Project) {

  public var server: BspServer? = null
  private var bspProcess: Process? = null
  private var cancelActions: List<Cancelable>? = null

  // consider enclosing nicely in an object
  public var dialogBuildToolUsed: Boolean? = null
  public var dialogBuildToolName: String? = null
  public var dialogConnectionFile: LocatedBspConnectionDetails? = null
  public var bspConnectionDetailsGeneratorProvider: BspConnectionDetailsGeneratorProvider? = null

  public var toolName: String? = null
    private set

  public fun connect(connectionFile: LocatedBspConnectionDetails) {
    val process = createAndStartProcess(connectionFile.bspConnectionDetails)
    val bspConsoleService = BspConsoleService.getInstance(project)

    val client = BspClient(
      bspConsoleService.bspSyncConsole,
      bspConsoleService.bspBuildConsole,
      bspConsoleService.bspRunConsole,
      bspConsoleService.bspTestConsole
    )

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
    val bspSyncConsole: TaskConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.startTask("bsp-obtain-config", "Obtain config", "Obtaining...")
    if (dialogBuildToolUsed != null) {
      if (dialogBuildToolUsed!!) {
        val xd1 = bspConnectionDetailsGeneratorProvider!!.generateBspConnectionDetailFileForGeneratorWithName(
          dialogBuildToolName!!,
          ConsoleOutputStream("bsp-obtain-config", bspSyncConsole)
        )
        val xd2 = LocatedBspConnectionDetailsParser.parseFromFile(xd1!!)
        this.toolName = xd2?.bspConnectionDetails?.name
        bspUtilService.bspConnectionDetails[project.locationHash] = xd2!!
        connect(xd2)
      } else {
        this.toolName = dialogConnectionFile?.bspConnectionDetails?.name
        bspUtilService.bspConnectionDetails[project.locationHash] = dialogConnectionFile!!
        connect(dialogConnectionFile!!)
      }
      bspSyncConsole.finishTask("bsp-obtain-config", "Config obtained!", SuccessResultImpl())
    }
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
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole
) {

  public fun runTarget(targetId: BuildTargetIdentifier): RunResult {

    val uuid = "run-" + UUID.randomUUID().toString()

    val runParams = RunParams(targetId).apply {
      originId = uuid
      arguments = listOf()
    }
    return server.buildTargetRun(runParams).get()
  }

  public fun buildTargets(targetIds: List<BuildTargetIdentifier>): CompileResult {

    val uuid = "build-" + UUID.randomUUID().toString()
    val startBuildMessage: String =
      if (targetIds.size == 1) "Building ${targetIds.first().uri}"
//      else if (targetIds.isEmpty()) "?"  // consider implementing
      else "Building ${targetIds.size} target(s)"
    bspBuildConsole.startTask(uuid, "Build", startBuildMessage)

    println("buildTargetCompile")
    val compileParams = CompileParams(targetIds).apply { originId = uuid }
    val compileResult = server.buildTargetCompile(compileParams).catchBuildErrors(uuid).get()

    when (compileResult.statusCode) {
      StatusCode.OK -> bspBuildConsole.finishTask(taskId = uuid, "Successfully completed!")
      StatusCode.CANCELLED -> bspBuildConsole.finishTask(taskId = uuid, "Cancelled!")
      StatusCode.ERROR -> bspBuildConsole.finishTask(uuid, "Ended with an error!", FailureResultImpl())
      else -> bspBuildConsole.finishTask(taskId = uuid, "Finished!")
    }

    return compileResult
  }

  public fun testTarget(targetId: BuildTargetIdentifier): TestResult {
    val params = TestParams(listOf(targetId))
    params.arguments = emptyList()
    params.originId = "test-" + UUID.randomUUID().toString()
    return server.buildTargetTest(params).get()
  }

  public fun buildTarget(targetId: BuildTargetIdentifier): CompileResult {
    return buildTargets(listOf(targetId))
  }

  public fun collectModel(): ProjectDetails {
    val syncId = syncId()
    bspSyncConsole.startTask(syncId, "Import", "Importing...")

    println("buildInitialize")
    val initializeBuildResult =
      server.buildInitialize(createInitializeBuildParams()).catchSyncErrors(syncId).get()

    println("onBuildInitialized")
    server.onBuildInitialized()

    server.onBuildInitialized()
    val projectDetails = collectModelWithCapabilities(initializeBuildResult.capabilities, syncId)

    bspSyncConsole.finishTask(syncId, "Import done!", SuccessResultImpl())

    println("done done!")
    return projectDetails
  }

  private fun collectModelWithCapabilities(
    buildServerCapabilities: BuildServerCapabilities,
    syncId: String
  ): ProjectDetails {
    println("workspaceBuildTargets")
    val workspaceBuildTargetsResult = server.workspaceBuildTargets().catchSyncErrors(syncId).get()
    val allTargetsIds = workspaceBuildTargetsResult!!.targets.map(BuildTarget::getId)

    println("buildTargetSources")
    val sourcesResult = server.buildTargetSources(SourcesParams(allTargetsIds)).catchSyncErrors(syncId).get()

    println("buildTargetResources")
    val resourcesResult =
      if (buildServerCapabilities.resourcesProvider) server.buildTargetResources(ResourcesParams(allTargetsIds))
        .catchSyncErrors(syncId).get() else null

    println("buildTargetDependencySources")
    val dependencySourcesResult =
      if (buildServerCapabilities.dependencySourcesProvider) server.buildTargetDependencySources(
        DependencySourcesParams(allTargetsIds)
      ).catchSyncErrors(syncId).get() else null

    println("buildTargetJavacOptions")
    val buildTargetJavacOptionsResult =
      server.buildTargetJavacOptions(JavacOptionsParams(allTargetsIds)).catchSyncErrors(syncId).get()

    println("done done!")
    return ProjectDetails(
      targetsId = allTargetsIds,
      targets = workspaceBuildTargetsResult.targets.toSet(),
      sources = sourcesResult.items,
      resources = resourcesResult?.items ?: emptyList(),
      dependenciesSources = dependencySourcesResult?.items ?: emptyList(),
      javacOptions = buildTargetJavacOptionsResult.items
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

  private fun <T> CompletableFuture<T>.catchSyncErrors(syncId: String): CompletableFuture<T> {
    return this
      .whenComplete { _, exception ->
        exception?.let {
          bspSyncConsole.addMessage(syncId(), "Sync failed")
          bspSyncConsole.finishTask(syncId, "Failed", FailureResultImpl(exception))
        }
      }
  }

  private fun <T> CompletableFuture<T>.catchBuildErrors(buildId: String): CompletableFuture<T> {
    return this
      .whenComplete { _, exception ->
        exception?.let {
          bspBuildConsole.addMessage("bsp-build", "Build failed")
          bspBuildConsole.finishTask(buildId, "Failed", FailureResultImpl(exception))
        }
      }
  }
}

private class BspClient(
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole,
  private val bspRunConsole: BspTargetRunConsole,
  private val bspTestConsole: BspTargetTestConsole,
) : BuildClient {

  override fun onBuildShowMessage(params: ShowMessageParams) {
    println("onBuildShowMessage")
    println(params)
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    println("onBuildLogMessage")
    println(params)
    addMessageToConsole(params.originId, params.message)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    when (params?.dataKind) {
      TaskDataKind.TEST_START -> {
        val gson = Gson()
        val testStart = gson.fromJson(params.data as JsonObject, TestStart::class.java)
        val isSuite = params.message.take(3) == "<S>"
        println("TEST START: ${testStart?.displayName}")
        bspTestConsole.startTest(isSuite, testStart.displayName)
      }

      TaskDataKind.TEST_TASK -> {
        // ignore
      }
    }
    println("onBuildTaskStart")
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println("onBuildTaskProgress")
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    when (params?.dataKind) {
      TaskDataKind.TEST_FINISH -> {
        val gson = Gson()
        val testFinish = gson.fromJson(params.data as JsonObject, TestFinish::class.java)
        val isSuite = params.message.take(3) == "<S>"
        println("TEST FINISH: ${testFinish?.displayName}")
        when (testFinish.status) {
          TestStatus.FAILED -> bspTestConsole.failTest(testFinish.displayName, testFinish.message)
          TestStatus.PASSED -> bspTestConsole.passTest(isSuite, testFinish.displayName)
          else -> bspTestConsole.ignoreTest(testFinish.displayName)
        }
      }

      TaskDataKind.TEST_REPORT -> {}
    }
    println("onBuildTaskFinish")
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    println("onBuildPublishDiagnostics")
    println(params)
    addDiagnosticToConsole(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println("onBuildTargetDidChange")
    println(params)
  }

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true) {
      bspBuildConsole.addMessage(originId, message)
    } else if (originId?.startsWith("test") == true) {
      bspTestConsole.print(message)
    } else if (originId?.startsWith("run") == true) {
      bspRunConsole.print(message)
    } else {
      bspSyncConsole.addMessage(originId ?: syncId(), message)
    }
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    if (params.originId?.startsWith("build") == true) {
      bspBuildConsole.addDiagnosticMessage(params)
    } else {
      bspSyncConsole.addDiagnosticMessage(params)
    }
  }
}
