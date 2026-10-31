package com.marzec.screen.pickitemscreen

import com.marzec.delegate.WithSearch
import com.marzec.delegate.WithSelection
import com.marzec.mvi.State
import com.marzec.view.SearchState

data class PickItemData<ITEM : Any>(
    val options: PickItemOptions<ITEM>,
    val items: List<ITEM>,
    override val selected: Set<String>,
    override val search: SearchState,
    val isComplete: Boolean = false,
    val error: String? = null,
) : WithSelection<String, PickItemData<ITEM>>,
    WithSearch<PickItemData<ITEM>> {

    override fun copyWithSelection(selected: Set<String>): PickItemData<ITEM> =
        copy(selected = selected)

    override fun allIds(): Set<String> = items.map { options.mapItemToId(it) }.toSet()

    override fun copyWithSearch(search: SearchState): PickItemData<ITEM> = copy(search = search)

    companion object {
        fun <ITEM : Any> default(options: PickItemOptions<ITEM>) = PickItemData(
            options = options,
            items = emptyList(),
            selected = emptySet(),
            search = SearchState.DEFAULT
        )

        fun <ITEM : Any> initial(
            options: PickItemOptions<ITEM>
        ) = State.Data(
            default(options).copy(selected = options.selected)
        )
    }
}

