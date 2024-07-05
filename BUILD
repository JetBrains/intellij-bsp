load(
    "@rules_intellij//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load(
    "@rules_intellij//intellij_platform_sdk:build_defs.bzl",
    "select_for_ide",
    "select_from_plugin_api_directory",
)
load(
    "@rules_intellij//build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)
load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
load("//:versions.bzl", "INTELLIJ_BSP_VERSION")

# Changelog file
filegroup(
    name = "changelog",
    srcs = ["CHANGELOG.md"],
)

define_kt_toolchain(
    name = "kotlin_toolchain",
    api_version = "1.9",  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
    jvm_target = "17",  # "1.6", "1.8", "9", "10", "11", "12", "13", "15", "16", "17", "18", "19", "20" or "21"
    language_version = "1.9",  # "1.1", "1.2", "1.3", "1.4", "1.5" "1.6", "1.7", "1.8", or "1.9"
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_deps = [],
    plugin_xmls = ["//src:main/resources/META-INF/base.xml"],
    visibility = ["//visibility:public"],
    deps = [
        "//jps-compilation/src:jps-compilation",
        "//protocol/src:protocol",
        "//src:intellij-bsp",
        "//workspacemodel/src:workspacemodel",
    ],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "CHANGELOG.md",
    description_file = "description.html",
    plugin_id = "org.jetbrains.bsp",
    plugin_name = "Build Server Protocol (BSP)",
    stamp_since_build = True,
    stamp_until_build = True,
    version = INTELLIJ_BSP_VERSION,
)

intellij_plugin(
    name = "intellij-bsp",
    plugin_xml = ":stamped_plugin_xml",
    tags = [],
    visibility = ["//visibility:public"],
    deps = [
        ":plugin_library",
    ] + select_for_ide(
        intellij = [],
        intellij_ue = [],
    ),
)

intellij_plugin_debug_target(
    name = "ijwb_bazel_dev",
    visibility = ["//visibility:public"],
    deps = [
        ":intellij-bsp_jar",
    ],
)

repackaged_files(
    name = "intellij-bsp_jar",
    srcs = [":intellij-bsp"],
    prefix = "intellij-bsp/lib",
    visibility = ["//visibility:public"],
)

plugin_deploy_zip(
    name = "intellij-bsp_zip",
    srcs = [
        ":intellij-bsp_jar",
    ],
    visibility = ["//visibility:public"],
    zip_filename = "intellij-bsp.zip",
)

alias(
    name = "format",
    actual = "//tools/format",
)
