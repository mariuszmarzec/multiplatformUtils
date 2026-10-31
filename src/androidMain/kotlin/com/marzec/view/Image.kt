package com.marzec.view

import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
actual fun Image(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    placeholderColor: Color,
    animationEnabled: Boolean
) {
    GlideImage(
        model = GlideUrlInterceptor.onUrl(url),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    ) { builder ->
        builder.placeholder(ColorDrawable(placeholderColor.toArgb()))
            .let { if (!animationEnabled) it.dontAnimate() else it }
    }
}