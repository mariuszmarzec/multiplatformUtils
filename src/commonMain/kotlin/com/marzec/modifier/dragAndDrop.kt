package com.marzec.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier


@Composable
expect fun Modifier.dragAndDrop(
    index: Int,
    dragEnteredIndex: MutableIntState,
    onDrop: (Int, Int) -> Unit
): Modifier