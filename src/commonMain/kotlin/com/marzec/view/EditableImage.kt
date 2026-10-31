package com.marzec.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import com.marzec.view.TextFieldStateful
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditableImage(
    url: String,
    imageModifier: Modifier,
    label: String,
    animationEnabled: Boolean = true,
    onApplyButtonClick: (url: String) -> Unit
) {
    var state: EditableImageState by remember { mutableStateOf(EditableImageState.Image) }

    EditableImage(
        url = url,
        imageModifier = imageModifier,
        state = state,
        label = label,
        animationEnabled = animationEnabled,
        onEditClick = { state = EditableImageState.Edit(url) },
        onUrlChanged = {
            state = (state as? EditableImageState.Edit)?.copy(url = it) ?: EditableImageState.Image
        },
        onCancelButtonClick = {
            state = EditableImageState.Image
        },
        onApplyButtonClick = {
            (state as? EditableImageState.Edit)?.let {
                onApplyButtonClick(it.url)
                state = EditableImageState.Image
            }
        }
    )
}

@Composable
fun EditableImage(
    url: String,
    imageModifier: Modifier,
    state: EditableImageState,
    label: String,
    animationEnabled: Boolean = true,
    onUrlChanged: (url: String) -> Unit,
    onEditClick: () -> Unit,
    onCancelButtonClick: () -> Unit,
    onApplyButtonClick: () -> Unit
) {
    Box {
        when (state) {
            is EditableImageState.Image -> {
                Image(
                    url = url,
                    modifier = imageModifier,
                    contentDescription = label,
                    animationEnabled = animationEnabled
                )
                IconButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = { onEditClick() }
                ) {
                    Icon(Icons.Default.Edit, "Edit $label")
                }
            }

            is EditableImageState.Edit -> {
                Column (modifier = Modifier.align(Alignment.Center)) {
                    TextFieldStateful(
                        value = state.url,
                        label = { Text(label) },
                        onValueChange = {
                            onUrlChanged(it)
                        }
                    )
                    Row {
                        OutlinedButton({ onCancelButtonClick() }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(16.dp))
                        Button({ onApplyButtonClick() }) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

sealed class EditableImageState {
    object Image : EditableImageState()

    data class Edit(
        val url: String
    ) : EditableImageState()
}
