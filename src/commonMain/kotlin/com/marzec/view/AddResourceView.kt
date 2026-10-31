package com.marzec.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun AddResourceView(
    showEmptyState: Boolean,
    onClick: () -> Unit,
    onRemoveAllButtonClick: () -> Unit,
    clearButtonContentDescription: String,
    label: String,
    emptyLabel: String
) {
    if (!showEmptyState) {
        Row(
            modifier = Modifier.testTag(AddResourceViewPageObject.contentRow).clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({ onRemoveAllButtonClick() }) {
                Icon(Icons.Default.Clear, clearButtonContentDescription)
            }
            Text(
                text = label
            )
        }
    } else {
        Button(
            modifier = Modifier.testTag(AddResourceViewPageObject.emptyLabel),
            onClick = {
            onClick()
        }) {
            Text(emptyLabel)
        }
    }
}

object AddResourceViewPageObject {
    val emptyLabel = "emptyLabel"
    val contentRow = "contentRow"
}