package com.marzec.modifier

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.dragAndDrop(
    index: Int,
    dragEnteredIndex: MutableIntState,
    onDrop: (draggedIndex: Int, targetIndex: Int) -> Unit
): Modifier = this
    .dragAndDropTarget(
        shouldStartDragAndDrop = {
            true
        },
        target = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val draggedIndex =
                    event.toAndroidDragEvent().localState as Int
                onDrop(draggedIndex, index)
                dragEnteredIndex.intValue = -1
                return true
            }

            override fun onEntered(event: DragAndDropEvent) {
                super.onEntered(event)
                dragEnteredIndex.intValue = index
            }

            override fun onExited(event: DragAndDropEvent) {
                super.onExited(event)
                dragEnteredIndex.intValue = -1
            }
        }
    )
    .dragAndDropSource { _ ->
        DragAndDropTransferData(
            ClipData(
                "",
                arrayOf(),
                ClipData.Item("")
            ),
            localState = index
        )
    }