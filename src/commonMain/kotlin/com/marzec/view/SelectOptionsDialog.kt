package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SelectOptionsDialog(state: Dialog.SelectOptionsDialog) {
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

            LazyColumn {
                items(
                    items = state.items,
                ) {
                    key(it.id) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextListItemView(
                                state = it,
                                onClickListener = {
                                    state.onItemClicked(it.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
