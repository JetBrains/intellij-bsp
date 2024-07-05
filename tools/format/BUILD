load("@aspect_rules_lint//format:defs.bzl", "format_multirun")
load("@rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary")

format_multirun(
    name = "format",
    kotlin = ":ktfmt",
    starlark = "@buildifier_prebuilt//:buildifier",
    visibility = ["//visibility:public"],
)

java_binary(
    name = "ktfmt",
    main_class = "com.facebook.ktfmt.cli.Main",
    runtime_deps = ["@ktfmt//jar"],
)
