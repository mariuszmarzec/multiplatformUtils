package com.marzec.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun CustomDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onCloseRequest = onDismissRequest) {
        content()
    }
}
