package com.marzec.view

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.marzec.cache.Cache
import com.marzec.logger.Logger
import com.marzec.resource.generated.resources.Res
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.File
import kotlin.collections.contains
import kotlin.collections.firstOrNull
import kotlin.collections.listOf
import kotlin.collections.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.animatedimage.AnimatedImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as ImageUtil

class ImageLoaderDesktop(
    private val clientProvider: () -> HttpClient,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher
) : ImageLoader {

    private val job = Job()
    private val scope = CoroutineScope(job)
    private val commands = MutableSharedFlow<suspend () -> Unit>(extraBufferCapacity = 200)

    init {
        scope.launch {
            commands.collect {
                it()
            }
        }
    }

    override suspend fun loadPicture(url: String): Flow<Image?> = withContext(dispatcher) {
        scope.launch {
            if (memoryCache.get<Image>(url) == null) {
                commands.emit { loadImage(url) }
            }
        }
        memoryCache.observe<Image>(url)
    }

    private suspend fun loadImage(url: String): Unit = withContext(dispatcher) {
        try {
            val cachedImage = getCachedFileOrNull(url)
            if (cachedImage != null) {
                putToMemoryCache(
                    url = url,
                    image = Image(
                        bytes = cachedImage.readBytes(),
                        extension = cachedImage.extension
                    )
                )
            } else {
                if (isNetworkLoading(url)) {
                    loadFromNetwork(url)
                } else {
                    loadFromResource(url)
                }
            }
        } catch (exception: Exception) {
            Logger.log("ImageLoader", exception.message.orEmpty(), exception)
        }
    }

    private suspend fun putToMemoryCache(url: String, image: Image) {
        memoryCache.put(url, image)
    }

    private fun isNetworkLoading(uri: String): Boolean = uri.startsWith("http")

    private suspend fun loadFromNetwork(url: String) {
        clientProvider().use { client ->
            val httpResponse: HttpResponse = client.get(url)
            val loadedImage = httpResponse.body<ByteArray>()
            val contentType = httpResponse.contentType()

            putToMemoryCache(url, Image(loadedImage, contentType?.contentSubtype.orEmpty()))
            when (contentType) {
                in supportedContentTypes -> {
                    saveImageToFile(loadedImage, url, contentType?.contentSubtype.orEmpty())
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
    private suspend fun loadFromResource(path: String) {
        val file = File(path)
        val extension = file.extension
        val loadedImage = Res.readBytes(path)
        require(loadedImage.isNotEmpty())
        require(extension.isNotEmpty())
        putToMemoryCache(path, Image(loadedImage, extension))
    }

    private fun getCachedFileOrNull(url: String) =
        supportedImagesExtensions.map { urlToFile(url, it) }.firstOrNull {
            it.exists()
        }

    companion object {
        private val supportedContentTypes =
            listOf(ContentType.Image.GIF, ContentType.Image.JPEG, ContentType.Image.PNG)

        private val supportedImagesExtensions = listOf("gif", "png", "jpeg", "jpg")
    }
}

private fun saveImageToFile(
    image: ByteArray,
    url: String,
    extension: String
) {
    val imagesDir = File(IMAGES_DIR)
    if (!imagesDir.exists()) {
        imagesDir.mkdir()
    }

    val imageFile = urlToFile(url, extension)
    imageFile.writeBytes(image)
}

private fun urlToFile(url: String, extension: String): File {
    val imagesDir = File(IMAGES_DIR)
    val fileName = url.hashCode()
    return File(imagesDir.absolutePath + File.separator + fileName + "." + extension)
}

fun ByteArray.toImageBitmap() = ImageUtil.makeFromEncoded(this).toComposeImageBitmap()

fun Image.isAnimated() = extension == "gif"

fun Image.toAnimatedImage(): AnimatedImage {
    val data = Data.makeFromBytes(bytes)
    val codec = Codec.makeFromData(data)
    return AnimatedImage(codec)
}