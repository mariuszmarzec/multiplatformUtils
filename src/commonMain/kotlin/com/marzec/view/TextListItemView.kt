package com.marzec.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.modifier.dragAndDrop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextListItemView(
    state: TextListItem,
    backgroundColor: Color = Color.Transparent,
    onLongClickListener: ((TextListItem) -> Unit)? = null,
    onClickListener: ((TextListItem) -> Unit)? = null,
    rightContent: @Composable () -> Unit = { }
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .let {
                if (onLongClickListener != null || onClickListener != null) {
                    it.combinedClickable(
                        onLongClick = onLongClickListener?.let<(TextListItem) -> Unit, () -> Unit> {
                            { it.invoke(state) }
                        },
                        onClick = onClickListener?.let {
                            { it.invoke(state) }
                        } ?: { }
                    )
                } else {
                    it
                }
            }
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = state.name,
                    fontSize = 16.sp,
                    style = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
                )
                if (state.description.isNotEmpty()) {
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = state.description,
                        fontSize = 14.sp,
                        style = TextStyle.Default
                    )
                }
            }
            rightContent()
        }
    }
}

data class TextListItem(
    val id: String,
    val name: String,
    val description: String
)
