package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import com.marzec.view.TextFieldStateful
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TextInputDialog(state: Dialog.TextInputDialog) {
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
            Text(state.title)

            Box(modifier = Modifier.padding(16.dp)) {
                TextFieldStateful(state.inputField, {
                    state.onTextChanged(it)
                })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    state.onConfirm(state.inputField)
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

