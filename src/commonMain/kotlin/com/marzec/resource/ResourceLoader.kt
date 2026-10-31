package com.marzec.resource

interface ResourceLoader {
    fun loadResource(name: String): String?

    fun getComposeResourceUri(path: String): String

    suspend fun loadCommonResourceBytes(path: String): ByteArray

    fun loadComonResourceFile(path: String): String? = loadResource("files/$path")
}
