load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")
load("//rules/kotlin:junit5.bzl", "kt_test")

kt_jvm_library(
    name = "plugin",
    srcs = glob([
        "main/kotlin/**/*.java",
        "main/kotlin/**/*.kt",
    ]),
    resources = glob([
        "main/resources/**",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api",
        "@rules_intellij//third_party/android",
        "@rules_intellij//third_party/python",
        "@maven//:com_google_code_gson_gson",
        "@maven//:ch_epfl_scala_bsp4j",
        "//protocol/src:protocol",
        "//workspacemodel/src:workspacemodel",
        "//jps-compilation/src:jps-compilation",
    ],
)


kt_jvm_test(
    name = "plugin_test",
    associates = [":plugin"],
    main_class = "org.junit.platform.console.ConsoleLauncher",
    args = ["--select-package=org.jetbrains", "--fail-if-no-tests"],
    srcs = glob([
        "test/kotlin/**/*.kt",
        "testFixtures/kotlin/**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_intellij//intellij_platform_sdk:plugin_api_for_tests",
        "@rules_intellij//intellij_platform_sdk:test_libs",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_junit_platform_junit_platform_console",
        "@maven//:io_kotest_kotest_assertions_api_jvm",
        "@maven//:io_kotest_kotest_assertions_core_jvm",
        "@maven//:io_kotest_kotest_assertions_shared_jvm",
        "@maven//:io_kotest_kotest_common_jvm",
    ],
)