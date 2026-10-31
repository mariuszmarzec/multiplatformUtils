package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TwoOptionsDialog(state: Dialog.TwoOptionsDialog, content: @Composable () -> Unit = {}) {
    CustomDialog(
        onDismissRequest = { state.onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = state.title,
                    fontSize = 16.sp,
                    style = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
                )
            }

            Text(text = state.message)

            content()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    state.onConfirm()
                }) {
                    Text(state.confirmButton)
                }
                TextButton(onClick = { state.onDismiss() }) {
                    Text(state.dismissButton)
                }
            }
        }
    }
}

@Composable
fun TwoOptionsDialogWithCheckbox(state: Dialog.TwoOptionsDialogWithCheckbox) {
    TwoOptionsDialog(state.twoOptionsDialog) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.checked,
                onCheckedChange = { state.onCheckedChange() }
            )
            TextButton({ state.onCheckedChange() }) {
                Text(state.checkBoxLabel)
            }
        }
    }
}
