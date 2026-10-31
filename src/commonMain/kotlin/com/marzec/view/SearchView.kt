package com.marzec.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.marzec.delegate.SearchDelegate

data class SearchState(
    val value: String,
    val focused: Boolean
) {
    companion object {
        val DEFAULT = SearchState(value = "", focused = false)
    }
}

@Composable
fun SearchView(state: SearchState, searchDelegate: SearchDelegate) {

    SearchViewState(
        state = state,
        onSearchFocusChanged = searchDelegate::onSearchFocusChanged,
        onSearchQueryChanged = searchDelegate::onSearchQueryChanged,
        onClearSearchClick = searchDelegate::clearSearch,
        onActivateSearchClick = searchDelegate::activateSearch
    )
}

@Composable
fun SearchViewState(
    state: SearchState,
    onSearchFocusChanged: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearchClick: () -> Unit,
    onActivateSearchClick: () -> Unit
) {
    Row(
        Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        val searchInUse = state.focused || state.value.isNotEmpty()
        BasicTextFieldStateful(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    onSearchFocusChanged(it.isFocused)
                }
                .widthIn(min = 100.dp, max = 300.dp)
                .padding(0.dp),
            singleLine = true,
            value = if (searchInUse) state.value else "Search",
            onValueChange = {
                if (searchInUse) {
                    onSearchQueryChanged(it)
                }
            }
        )
        LaunchedEffect(key1 = state.focused) {
            if (state.focused) {
                focusRequester.requestFocus()
            } else {
                focusManager.clearFocus()
            }
        }
        if (searchInUse) {
            IconButton({ onClearSearchClick() }) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search"
                )
            }
        } else {
            IconButton({ onActivateSearchClick() }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Activate search"
                )
            }
        }
    }
}

