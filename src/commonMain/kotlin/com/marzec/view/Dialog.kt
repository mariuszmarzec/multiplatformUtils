package com.marzec.view

import androidx.compose.runtime.Composable


@Composable
fun DialogBox(
    state: Dialog
) {
    when (state) {
        is Dialog.TextInputDialog -> TextInputDialog(state)
        is Dialog.TwoOptionsDialog -> TwoOptionsDialog(state)
        is Dialog.SelectOptionsDialog -> SelectOptionsDialog(state)
        is Dialog.TwoOptionsDialogWithCheckbox -> TwoOptionsDialogWithCheckbox(state)
        is Dialog.NoDialog -> Unit
    }
}

sealed class Dialog {

    data class TextInputDialog(
        val title: String,
        val inputField: String,
        val confirmButton: String,
        val dismissButton: String,
        val onTextChanged: (String) -> Unit = {},
        val onDismiss: () -> Unit = {},
        val onConfirm: (String) -> Unit = {},
    ) : Dialog()

    data class TwoOptionsDialog(
        val title: String,
        val message: String,
        val confirmButton: String,
        val dismissButton: String,
        val onDismiss: () -> Unit = {},
        val onConfirm: () -> Unit = {},
    ) : Dialog()

    data class TwoOptionsDialogWithCheckbox(
        val twoOptionsDialog: TwoOptionsDialog,
        val checked: Boolean,
        val checkBoxLabel: String,
        val onCheckedChange: () -> Unit = {},
    ) : Dialog()

    data class SelectOptionsDialog(
        val items: List<TextListItem>,
        val onDismiss: () -> Unit = {},
        val onItemClicked: (String) -> Unit = {}
    ) : Dialog()

    object NoDialog: Dialog()
}
