package com.marzec.resource

import com.marzec.resource.generated.resources.Res
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertTrue

class ComposeResourcesTest {

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun `Vitalia JPEG and animated GIF are available through Compose resources`() = runTest {
        val thumbnail = Res.readBytes("files/vitalia/thumbnail_100.jpg")
        val animation = Res.readBytes("files/vitalia/anim_100.gif")
        val resourceLoader = ResourceLoaderImpl()

        assertTrue(thumbnail.isNotEmpty())
        assertTrue(animation.isNotEmpty())
        assertTrue(resourceLoader.getComposeResourceUri("vitalia/thumbnail_100.jpg").isNotBlank())
        assertTrue(resourceLoader.getComposeResourceUri("vitalia/anim_100.gif").isNotBlank())
    }
}
