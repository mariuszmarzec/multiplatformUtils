package com.marzec.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.AnimatedImage
import kotlin.String
import kotlin.runCatching

const val IMAGES_DIR = "images"

/**
 * Result of safely decoding an image. Encapsulates the fallback behavior:
 * if decoding fails (malformed/corrupt image), returns null instead of propagating the exception.
 */
internal sealed interface DecodedImage {
    data class Static(val bitmap: ImageBitmap) : DecodedImage
    data class Animated(val animatedImage: AnimatedImage) : DecodedImage
    object Failure : DecodedImage
}

/**
 * Safely performs the non-composable decode/conversion work for an [Image].
 * Failures during decode (malformed/corrupt data, unsupported format) are caught and
 * result in [DecodedImage.Failure], preserving the previous fallback semantics.
 * Composable functions (like [animate]) are NOT invoked here.
 */
internal fun decodeImageSafely(image: Image, animationEnabled: Boolean): DecodedImage {
    return if (animationEnabled && image.isAnimated()) {
        runCatching {
            // Non-composable: create the AnimatedImage object from bytes
            val animatedImage = image.toAnimatedImage()
            DecodedImage.Animated(animatedImage)
        }.getOrElse { DecodedImage.Failure }
    } else {
        runCatching {
            // Non-composable: decode bytes to ImageBitmap
            val bitmap = image.bytes.toImageBitmap()
            DecodedImage.Static(bitmap)
        }.getOrElse { DecodedImage.Failure }
    }
}

@Composable
actual fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderColor: Color,
    animationEnabled: Boolean
) {
    ImageInternal(
        url,
        contentDescription,
        modifier,
        contentScale,
        ColorPainter(placeholderColor),
        animationEnabled = animationEnabled
    )
}

@Composable
private fun ImageInternal(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: Painter = ColorPainter(Color.Gray),
    imageLoader: ImageLoader = ImageLoader.imageLoader,
    animationEnabled: Boolean = true
) {
    val decoded = loadImage(url, imageLoader, animationEnabled)

    when (decoded) {
        is DecodedImage.Static -> {
            Image(
                modifier = modifier,
                painter = BitmapPainter(decoded.bitmap),
                contentDescription = contentDescription,
                contentScale = contentScale
            )
        }
        is DecodedImage.Animated -> {
            // animate() is a @Composable that returns ImageBitmap - safe to call here in composable context
            Image(
                modifier = modifier,
                bitmap = decoded.animatedImage.animate(),
                contentDescription = contentDescription,
                contentScale = contentScale
            )
        }
        DecodedImage.Failure -> {
            Image(
                modifier = modifier,
                painter = placeholder,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
private fun loadImage(
    url: String,
    imageLoader: ImageLoader,
    animationEnabled: Boolean
): DecodedImage {
    val image by loadImageState(url, imageLoader)

    return image?.let { loadedImage ->
        decodeImageSafely(loadedImage, animationEnabled)
    } ?: DecodedImage.Failure
}

@Composable
fun loadImageState(
    url: String,
    imageLoader: ImageLoader
): State<Image?> =
    produceState(
        initialValue = runBlocking { ImageLoader.imageMemoryCache.get(url) },
        key1 = url,
        key2 = imageLoader
    ) {
        imageLoader.loadPicture(url).collect { image ->
            value = image
        }
    }
