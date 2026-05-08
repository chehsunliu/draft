package io.github.chehsunliu.itx.impl

internal fun requireEnv(name: String): String = System.getenv(name) ?: error("missing env var: $name")

internal fun envInt(
    name: String,
    default: Int,
): Int = System.getenv(name)?.takeIf { it.isNotBlank() }?.toInt() ?: default
