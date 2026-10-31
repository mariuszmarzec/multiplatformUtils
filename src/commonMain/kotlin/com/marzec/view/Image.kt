package com.marzec.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.marzec.cache.Cache
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

object GlideUrlInterceptor {
    var onUrl: (String) -> String = { it }
}

@Composable
expect fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholderColor: Color = Color.Gray,
    animationEnabled: Boolean = false
)

interface ImageLoader {
    suspend fun loadPicture(url: String): Flow<Image?>

    companion object : ImageLoader {

        lateinit var imageMemoryCache: Cache
        lateinit var ioDispatcher: CoroutineDispatcher
        lateinit var clientProvider: () -> HttpClient

        lateinit var imageLoader: ImageLoader

        override suspend fun loadPicture(url: String): Flow<Image?> =
            imageLoader.loadPicture(url)
    }
}

open class CompositeImageLoader(private val imageLoader: ImageLoader) : ImageLoader by imageLoader

data class Image(
    val bytes: ByteArray,
    val extension: String
)
