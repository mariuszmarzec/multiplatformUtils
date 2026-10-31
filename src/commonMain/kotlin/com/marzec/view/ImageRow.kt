package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ImageRow(
    url: String,
    contentDescription: String = "",
    backgroundColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {

    Row(
        modifier = Modifier
            .background(backgroundColor)
            .let {
                if (onClick != null) {
                    it.clickable { onClick() }
                } else {
                    it
                }
            }
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            url = url,
            modifier = Modifier.width(48.dp).height(48.dp),
            contentDescription = contentDescription,
            contentScale = ContentScale.Inside,
            animationEnabled = false
        )
        content()
    }
}
