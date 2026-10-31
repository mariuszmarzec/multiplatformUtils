package com.marzec.resource

import android.content.Context
import android.net.Uri
import com.marzec.common.readFile
import com.marzec.resource.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

class ResourceLoaderImpl(private val context: Context) : ResourceLoader {

    override fun loadResource(name: String): String? = context.readFile(name)

    @OptIn(ExperimentalResourceApi::class)
    override fun getComposeResourceUri(path: String): String = Res.getUri("files/$path")

    @OptIn(ExperimentalResourceApi::class)
    override fun loadComonResourceFile(path: String): String? {
        val uri = Res.getUri("files/$path")
        val resourcePath = Uri.parse(uri).path ?: return null
        val assetsPrefix = "/android_asset/"
        if (!resourcePath.startsWith(assetsPrefix)) return null
        return context.assets.open(resourcePath.removePrefix(assetsPrefix)).bufferedReader().use { it.readText() }
    }
}
