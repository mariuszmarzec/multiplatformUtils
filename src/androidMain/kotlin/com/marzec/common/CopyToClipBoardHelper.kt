package com.marzec.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

actual class CopyToClipBoardHelper(private val context: Context) {

    actual fun copy(text: String) {
        context.getSystemService<ClipboardManager>()
            ?.setPrimaryClip(ClipData.newPlainText("label", text))
    }
}