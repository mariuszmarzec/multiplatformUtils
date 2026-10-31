package com.marzec.view

import androidx.compose.runtime.Composable

@Composable
expect fun CustomDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit)
