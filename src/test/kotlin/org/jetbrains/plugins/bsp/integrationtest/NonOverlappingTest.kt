package org.jetbrains.plugins.bsp.integrationtest

import com.google.gson.Gson
import com.jetbrains.bsp.bsp4kt.BspConnectionDetails
import com.jetbrains.bsp.bsp4kt.BuildClient
import com.jetbrains.bsp.bsp4kt.BuildClientCapabilities
import com.jetbrains.bsp.bsp4kt.DidChangeBuildTarget
import com.jetbrains.bsp.bsp4kt.InitializeBuildParams
import com.jetbrains.bsp.bsp4kt.LogMessageParams
import com.jetbrains.bsp.bsp4kt.PublishDiagnosticsParams
import com.jetbrains.bsp.bsp4kt.ShowMessageParams
import com.jetbrains.bsp.bsp4kt.TaskFinishParams
import com.jetbrains.bsp.bsp4kt.TaskProgressParams
import com.jetbrains.bsp.bsp4kt.TaskStartParams
import com.jetbrains.jsonrpc4kt.Launcher
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.NonOverlappingTargets
import org.jetbrains.magicmetamodel.impl.OverlappingTargetsGraph
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.tasks.calculateProjectDetailsWithCapabilities
import org.jetbrains.plugins.bsp.utils.withRealEnvs
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

// TODO make sure these values are updated by dependabot or similar tool
private const val BAZEL_REPOSITORY_TAG = "6.0.0"
private const val BAZEL_EXECUTABLE_VERSION = "5.4.0"
private const val BAZEL_BSP_VERSION = "2.7.1"

@OptIn(ExperimentalTime::class)
class NonOverlappingTest {
  @Test
  fun `Compute non overlapping targets for bazelbuild_bazel project`() {
    val bazelDir = createTempDirectory("bazel-bsp-")
    cloneRepository(bazelDir, BAZEL_REPOSITORY_TAG)
    setBazelVersion(bazelDir, BAZEL_EXECUTABLE_VERSION)
    installBsp(bazelDir, "//...")
    val connectionDetails =
      Gson().fromJson(bazelDir.resolve(".bsp/bazelbsp.json").readText(), BspConnectionDetails::class.java)
    val bspServerProcess = bspProcess(connectionDetails, bazelDir)
    try {
      val launcher = startBsp(bspServerProcess)
      val params = initializeParams(bazelDir)
      launcher.startListening()
      val server = launcher.remoteProxy
      val initializationResult = server.buildInitialize(params).get()
      server.onBuildInitialized()
      val projectDetails =
        calculateProjectDetailsWithCapabilities(server, initializationResult.capabilities, { println(it) })!!
      val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
      val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)
      val targets = projectDetails.targets.map { it.toBuildTargetInfo() }.toSet()
      val nonOverlapping = measureTimedValue { NonOverlappingTargets(targets, overlappingTargetsGraph) }
      nonOverlapping.value.size shouldBe 1958
      println("Computing non-overlapping targets took ${nonOverlapping.duration}")
    } finally {
      bspServerProcess.destroyForcibly()
    }
  }

  private fun setBazelVersion(bazelDir: Path, bazelVersion: String) {
    bazelDir.resolve(".bazelversion").writeText(bazelVersion)
  }

  private fun cloneRepository(bazelDir: Path, gitRevision: String) {
    ProcessBuilder(
      "git", "clone",
      "--branch", gitRevision,
      "--depth", "1",
      "https://github.com/bazelbuild/bazel",
      bazelDir.toAbsolutePath().toString()
    )
      .inheritIO()
      .start()
      .run {
        waitFor(3, TimeUnit.MINUTES)
        if (exitValue() != 0) error("Could not clone")
      }
  }

  private fun installBsp(bazelDir: Path, target: String) {
    ProcessBuilder(
      "cs", "launch", "org.jetbrains.bsp:bazel-bsp:$BAZEL_BSP_VERSION",
      "-M", "org.jetbrains.bsp.bazel.install.Install",
      "--",
      "-t", target
    ).run {
      inheritIO()
      directory(bazelDir.toFile())
      start()
    }.run {
      waitFor(3, TimeUnit.MINUTES)
      if (exitValue() != 0) error("Could not setup BSP")
    }
  }

  private fun initializeParams(bazelDir: Path) = InitializeBuildParams(
    "IntelliJ-BSP",
    "0.0.1",
    "2.0.0",
    bazelDir.toUri().toString(),
    BuildClientCapabilities(listOf("java"))
  )

  private fun startBsp(bspServerProcess: Process): Launcher<DummyClient, BspServer> {
    return Launcher.Builder(
      bspServerProcess.inputStream,
      bspServerProcess.outputStream,
      DummyClient(),
      BspServer::class,
      executorService = Executors.newCachedThreadPool()
    ).create()
  }

  private fun bspProcess(connectionDetails: BspConnectionDetails, bazelDir: Path): Process {
    return ProcessBuilder(connectionDetails.argv)
      .directory(bazelDir.toFile())
      .withRealEnvs()
      .start()
  }
}

class DummyClient : BuildClient {
  override fun onBuildShowMessage(params: ShowMessageParams) {
    println(params)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    println(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    println(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    println(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    println(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    println(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget) {
    println(params)
  }
}
