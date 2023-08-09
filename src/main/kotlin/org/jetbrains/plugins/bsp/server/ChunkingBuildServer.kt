package org.jetbrains.plugins.bsp.server

import com.jetbrains.bsp.bsp4kt.BuildTargetIdentifier
import com.jetbrains.bsp.bsp4kt.CleanCacheParams
import com.jetbrains.bsp.bsp4kt.CleanCacheResult
import com.jetbrains.bsp.bsp4kt.DependencyModulesParams
import com.jetbrains.bsp.bsp4kt.DependencyModulesResult
import com.jetbrains.bsp.bsp4kt.DependencySourcesParams
import com.jetbrains.bsp.bsp4kt.DependencySourcesResult
import com.jetbrains.bsp.bsp4kt.JavacOptionsParams
import com.jetbrains.bsp.bsp4kt.JavacOptionsResult
import com.jetbrains.bsp.bsp4kt.OutputPathsParams
import com.jetbrains.bsp.bsp4kt.OutputPathsResult
import com.jetbrains.bsp.bsp4kt.ResourcesParams
import com.jetbrains.bsp.bsp4kt.ResourcesResult
import com.jetbrains.bsp.bsp4kt.SourcesParams
import com.jetbrains.bsp.bsp4kt.SourcesResult
import org.jetbrains.plugins.bsp.server.connection.BspServer
import java.util.concurrent.CompletableFuture
import kotlin.math.sqrt

private typealias BTI = BuildTargetIdentifier

public class ChunkingBuildServer<S : BspServer>(
  private val base: S,
  private val minChunkSize: Int,
) : BspServer by base {
  override fun buildTargetSources(params: SourcesParams): CompletableFuture<SourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { SourcesParams(it) },
      doRequest = { base.buildTargetSources(it) },
      unwrapRes = { it.items },
      wrapRes = { SourcesResult(it) },
    )(params)

  override fun buildTargetResources(params: ResourcesParams): CompletableFuture<ResourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { ResourcesParams(it) },
      doRequest = { base.buildTargetResources(it) },
      unwrapRes = { it.items },
      wrapRes = { ResourcesResult(it) },
    )(params)

  override fun buildTargetDependencySources(
    params: DependencySourcesParams
  ): CompletableFuture<DependencySourcesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { DependencySourcesParams(it) },
      doRequest = { base.buildTargetDependencySources(it) },
      unwrapRes = { it.items },
      wrapRes = { DependencySourcesResult(it) },
    )(params)

  override fun buildTargetOutputPaths(params: OutputPathsParams): CompletableFuture<OutputPathsResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { OutputPathsParams(it) },
      doRequest = { base.buildTargetOutputPaths(it) },
      unwrapRes = { it.items },
      wrapRes = { OutputPathsResult(it) },
    )(params)

  override fun buildTargetDependencyModules(
    params: DependencyModulesParams
  ): CompletableFuture<DependencyModulesResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { DependencyModulesParams(it) },
      doRequest = { base.buildTargetDependencyModules(it) },
      unwrapRes = { it.items },
      wrapRes = { DependencyModulesResult(it) },
    )(params)

  override fun buildTargetJavacOptions(params: JavacOptionsParams): CompletableFuture<JavacOptionsResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { JavacOptionsParams(it) },
      doRequest = { base.buildTargetJavacOptions(it) },
      unwrapRes = { it.items },
      wrapRes = { JavacOptionsResult(it) },
    )(params)

  override fun buildTargetCleanCache(params: CleanCacheParams): CompletableFuture<CleanCacheResult> =
    chunkedRequest(
      unwrapReq = { it.targets },
      wrapReq = { CleanCacheParams(it) },
      doRequest = { base.buildTargetCleanCache(it) },
      unwrapRes = { listOf(it) },
      wrapRes = { results ->
        CleanCacheResult(
          results.joinToString("\n") { it.message ?: "" },
          results.all { it.cleaned },
        )
      },
    )(params)

  private fun <ReqW, Res, ResW> chunkedRequest(
    unwrapReq: (ReqW) -> List<BTI>,
    wrapReq: (List<BTI>) -> ReqW,
    doRequest: (ReqW) -> CompletableFuture<ResW>,
    unwrapRes: (ResW) -> List<Res>,
    wrapRes: (List<Res>) -> ResW,
  ): (ReqW) -> CompletableFuture<ResW> =
    fun(requestParams: ReqW): CompletableFuture<ResW> {
      val allTargetsIds = unwrapReq(requestParams)
      val requests = allTargetsIds.chunked(chunkSize(allTargetsIds))
        .map { doRequest(wrapReq(it)) }
      val all = CompletableFuture.allOf(*requests.toTypedArray()).thenApply {
        requests.map { unwrapRes(it.get()) }.flatten().let { wrapRes(it) }
      }
      return all.whenComplete { _, _ ->
        if (all.isCancelled) requests.forEach { it.cancel(true) }
      }
    }

  private fun chunkSize(targetIds: List<Any>) =
    sqrt(targetIds.size.toDouble()).toInt().coerceAtLeast(minChunkSize)
}
