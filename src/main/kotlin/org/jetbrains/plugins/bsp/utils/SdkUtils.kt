package org.jetbrains.plugins.bsp.utils

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformanceSuspend
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import kotlin.io.path.toPath

internal object SdkUtils {
  suspend fun addJdkIfNeeded(projectName: String, javaHomeUri: String) {
    val jdkName = logPerformance("projectName.projectNameToJdkName(javaHomeUri)") {
      projectName.projectNameToJdkName(javaHomeUri)
    }
    val path = logPerformance("javaHomeUri.safeCastToURI().toPath().toString()") {
      javaHomeUri.safeCastToURI().toPath().toString()
    }
    val jdk = logPerformance("ExternalSystemJdkProvider.getInstance().createJdk(jdkName, path)") {
      ExternalSystemJdkProvider.getInstance().createJdk(jdkName, path)
    }
    addSdkIfNeeded(jdk)
  }

  suspend fun addSdkIfNeeded(sdk: Sdk) {
    val sdkTable = logPerformance("ProjectJdkTable.getInstance()") {
      ProjectJdkTable.getInstance()
    }
    val existingSdk = logPerformance("sdkTable.findJdk(sdk.name, sdk.sdkType.name)") {
      sdkTable.findJdk(sdk.name, sdk.sdkType.name)
    }
    println("Before log")
    logPerformanceSuspend("if blah writeAction") {
      println("before if")
      if (existingSdk == null || existingSdk.homePath != sdk.homePath) {
        println("Before write action")
        writeAction {
          println("before log performance existing jdk")
          logPerformance("existingSdk?.let { sdkTable.removeJdk(existingSdk) }") {
            println("inside log perfromance existing jdk")
            existingSdk?.let { sdkTable.removeJdk(existingSdk) }
          }
          logPerformance("sdkTable.addJdk(sdk)") {
            sdkTable.addJdk(sdk)
          }
          println("before write action finished")
        }
        println("write action finished")
      }
    }
    println("after log")
  }

  fun getProjectJdkOrMostRecentJdk(project: Project): Sdk? =
    ProjectRootManager.getInstance(project).projectSdk?.takeIf { it.sdkType == JavaSdk.getInstance() }
      ?: getMostRecentJdk()

  private fun getMostRecentJdk(): Sdk? {
    val jdkType = JavaSdk.getInstance()
    val jdks = ProjectJdkTable.getInstance().getSdksOfType(jdkType)
    return jdks.maxWithOrNull(jdkType.comparator)
  }
}
