package com.marzec.delegate

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.extensions.toggle
import com.marzec.mvi.StoreDelegate

interface WithSelection<SELECT_ITEM, DATA> {
    val selected: Set<SELECT_ITEM>
    fun copyWithSelection(selected: Set<SELECT_ITEM>): DATA
    fun allIds(): Set<SELECT_ITEM>
}

interface SelectionDelegate<SELECT_ITEM> {
    fun onSelectedChange(id: SELECT_ITEM)
    fun selectAll(ids: Set<SELECT_ITEM>)
    fun deselectAll(ids: Set<SELECT_ITEM>)
    fun onAllSelectClicked()
}

class SelectionDelegateImpl<SELECT_ITEM, DATA : WithSelection<SELECT_ITEM, DATA>> : StoreDelegate<State<DATA>>(),
    SelectionDelegate<SELECT_ITEM> {

    override fun selectAll(ids: Set<SELECT_ITEM>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected + ids.toSet())
            }
        }
    }

    override fun deselectAll(ids: Set<SELECT_ITEM>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected - ids.toSet())
            }
        }
    }

    override fun onSelectedChange(id: SELECT_ITEM) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected.toggle(id))
            }
        }
    }

    override fun onAllSelectClicked() = sideEffectIntent {
        state.ifDataAvailable {
            val ids = allIds()
            if (selected.size == ids.size) {
                deselectAll(ids)
            } else {
                selectAll(ids)
            }
        }
    }
}
