package com.marzec.resource

import com.marzec.resource.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.net.URI

class ResourceLoaderImpl : ResourceLoader {
    override fun loadResource(name: String): String? {
        return this::class.java.classLoader?.getResource(name)?.readText()
    }

    @OptIn(ExperimentalResourceApi::class)
    override fun getComposeResourceUri(path: String): String = Res.getUri("files/$path")

    @OptIn(ExperimentalResourceApi::class)
    override fun loadComonResourceFile(path: String): String? {
        val uri = Res.getUri("files/$path")
        val resourceUrl = runCatching { URI(uri).toURL() }.getOrNull() ?: return null
        return resourceUrl.openStream().bufferedReader().use { it.readText() }
    }
}
