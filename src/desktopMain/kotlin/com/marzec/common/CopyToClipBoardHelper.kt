package com.marzec.common

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual class CopyToClipBoardHelper {

    actual fun copy(text: String) {
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(text), null)
    }
}