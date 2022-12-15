package org.jetbrains.plugins.bsp.integrationtest

import ch.epfl.scala.bsp4j.*
import com.google.gson.Gson
import io.kotest.matchers.shouldBe
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.magicmetamodel.impl.NonOverlappingTargets
import org.jetbrains.magicmetamodel.impl.OverlappingTargetsGraph
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.tasks.calculateProjectDetailsWithCapabilities
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText

// TODO make sure these values are updated by dependabot or similar tool
private const val bazelRepositoryTag = "5.3.2"
private const val bazelBspVersion = "2.3.0"
class NonOverlapping {
  @Test
  fun `Compute non overlapping targets for bazelbuild_bazel project`() {
    val bazelDir = createTempDirectory("bazel-bsp-")
    cloneRepository(bazelDir)
    installBsp(bazelDir, "//src/tools/...")
    val connectionDetails = Gson().fromJson(bazelDir.resolve(".bsp/bazelbsp.json").readText(), BspConnectionDetails::class.java)
    val launcher = startBsp(connectionDetails, bazelDir)
    val params = initializeParams(bazelDir)
    launcher.startListening()
    val server = launcher.remoteProxy
    val initializationResult = server.buildInitialize(params).get()
    server.onBuildInitialized()
    val projectDetails = calculateProjectDetailsWithCapabilities(server, initializationResult.capabilities){}
    val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
    val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)
    val nonOverlapping = NonOverlappingTargets(projectDetails.targets, overlappingTargetsGraph)
    nonOverlapping.size shouldBe 60
  }

  private fun cloneRepository(bazelDir: Path) {
    ProcessBuilder("git", "clone",
      "--branch", bazelRepositoryTag,
      "--depth", "1",
      "https://github.com/bazelbuild/bazel",
      bazelDir.toAbsolutePath().toString())
      .inheritIO()
      .start().run {
        waitFor(3, TimeUnit.MINUTES)
        if (exitValue() != 0) throw RuntimeException("Could not clone")
      }
  }

  private fun installBsp(bazelDir: Path, target: String) {
    ProcessBuilder(
      "cs", "launch", "org.jetbrains.bsp:bazel-bsp:$bazelBspVersion",
      "-M", "org.jetbrains.bsp.bazel.install.Install",
      "--",
      "-t", target
    ).run {
      environment()["JAVA_HOME"] = "/usr/lib/jvm/java-11-openjdk"
      directory(bazelDir.toFile())
      start()
    }.run {
      waitFor(3, TimeUnit.MINUTES)
      if (exitValue() != 0) throw RuntimeException("Could not setup BSP")
    }
  }

  private fun initializeParams(bazelDir: Path) = InitializeBuildParams(
    "IntelliJ-BSP",
    "0.0.1",
    "2.0.0",
    bazelDir.toUri().toString(),
    BuildClientCapabilities(listOf("java"))
  )

  private fun startBsp(connectionDetails: BspConnectionDetails, bazelDir: Path): Launcher<BspServer> {
    val bspServerProcess = ProcessBuilder(connectionDetails.argv)
      .directory(bazelDir.toFile())
      .withRealEnvs()
      .start()
    return Launcher.Builder<BspServer>()
      .setRemoteInterface(BspServer::class.java)
      .setExecutorService(Executors.newFixedThreadPool(4))
      .setInput(bspServerProcess.inputStream)
      .setOutput(bspServerProcess.outputStream)
      .setLocalService(DummyClient())
      .create()
  }
}



class DummyClient : BuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams?) {
    println(params)
  }

  override fun onBuildLogMessage(params: LogMessageParams?) {
    println(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams?) {
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams?) {
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams?) {
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams?) {
    println(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    println(params)
  }
}