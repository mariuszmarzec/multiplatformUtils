package com.marzec.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/**
 * @param items - items, min 1 item
 * @param selectedItemIndex - if null first item is taken as selected
 */
@Composable
fun SpinnerView(
    label: String = "",
    items: List<String>,
    selectedItemIndex: Int?,
    onValueChanged: (Int) -> Unit
) {
    assert(items.isNotEmpty()) { "List is empty" }
    val selectedText = items[selectedItemIndex ?: 0]

    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = Icons.Filled.ArrowDropDown

    val onViewClickAction: () -> Unit = { expanded = !expanded }

    Column {
        if (label.isNotEmpty()) {
            Text(label)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onViewClickAction() }
                .onGloballyPositioned { coordinates ->
                    //This value is used to assign to the DropDown the same width
                    textFieldSize = coordinates.size.toSize()
                }.padding(8.dp)
        ) {
            Text(
                text = selectedText,
                modifier = Modifier
                    .defaultMinSize(minWidth = 120.dp)
            )
            Spacer(Modifier.width(8.dp))
            Icon(icon, "spinner expand")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            items.forEachIndexed { index, label ->
                DropdownMenuItem(onClick = {
                    onValueChanged(index)
                    expanded = false
                }) {
                    Text(text = label)
                }
            }
        }
    }
}