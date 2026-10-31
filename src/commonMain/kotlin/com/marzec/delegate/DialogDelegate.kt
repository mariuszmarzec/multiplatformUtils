package com.marzec.delegate

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.mvi.intent as createIntent
import com.marzec.extensions.asInstanceAndReturn
import com.marzec.mvi.Intent3
import com.marzec.mvi.StoreDelegate
import com.marzec.mvi.map

interface DialogDelegate<ID_TYPE> {
    fun closeDialog()
    fun showRemoveDialogWithCheckBox(idsToRemove: List<ID_TYPE>)
    fun showRemoveDialog(idsToRemove: List<ID_TYPE>, id: String = "")
    fun onRemoveWithSubTasksChange()
    fun showSelectUrlDialog(urls: List<String>)
}

class DialogDelegateImpl<ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> : StoreDelegate<State<DATA>>(),
    DialogDelegate<ID_TYPE> {

    override fun closeDialog() =
        run(createCloseDialog<ID_TYPE, DATA>().mapIntent())

    override fun showRemoveDialogWithCheckBox(idsToRemove: List<ID_TYPE>) =
        run(createShowRemoveDialogWithCheckBox<ID_TYPE, DATA>(idsToRemove).mapIntent())

    override fun showRemoveDialog(idsToRemove: List<ID_TYPE>, id: String) =
        run(createShowRemoveTaskDialog<ID_TYPE, DATA>(idsToRemove, id).mapIntent())

    override fun onRemoveWithSubTasksChange() =
        run(intentOnRemoveWithSubTasksChange<ID_TYPE, DATA>().mapIntent())

    override fun showSelectUrlDialog(urls: List<String>) =
        run(intentShowSelectUrlDialog<ID_TYPE, DATA>(urls).mapIntent())

    private fun Intent3<DATA, Any>.mapIntent(): Intent3<State<DATA>, Any> = map(
        stateReducer = { state.reduceData { it } },
        stateMapper = { it.ifDataAvailable { it.data } }
    )
}

private fun <ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> createCloseDialog(): Intent3<DATA, Any> =
    createIntent {
        reducer {
            state.copyWithDialog(DialogState.NoDialog())
        }
    }

private fun <ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> createShowRemoveDialogWithCheckBox(
    idsToRemove: List<ID_TYPE>
): Intent3<DATA, Any> = createIntent {
    reducer {
        state.copyWithDialog(dialog = DialogState.RemoveDialogWithCheckBox(idsToRemove))
    }
}

private fun <ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> createShowRemoveTaskDialog(
    idsToRemove: List<ID_TYPE>, id: String
): Intent3<DATA, Any> = createIntent {
    reducer {
        state.copyWithDialog(dialog = DialogState.RemoveDialog(idsToRemove, id))
    }
}

private fun <ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> intentOnRemoveWithSubTasksChange(): Intent3<DATA, Any> =
    createIntent {
        reducer {
            state.copyWithDialog(dialog = state.dialog.asInstanceAndReturn<DialogState.RemoveDialogWithCheckBox<ID_TYPE>> {
                copy(checked = !this.checked)
            } ?: DialogState.NoDialog())
        }
    }

private fun <ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> intentShowSelectUrlDialog(urls: List<String>): Intent3<DATA, Any> =
    createIntent {
        reducer {
            state.copyWithDialog(dialog = DialogState.SelectOptionsDialog(urls))
        }
    }

class DialogDelegateSimpleImpl<ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> :
    StoreDelegate<DATA>(),
    DialogDelegate<ID_TYPE> {

    override fun closeDialog() = run(createCloseDialog())

    override fun showRemoveDialogWithCheckBox(idsToRemove: List<ID_TYPE>) =
        run(createShowRemoveDialogWithCheckBox(idsToRemove))

    override fun showRemoveDialog(idsToRemove: List<ID_TYPE>, id: String) =
        run(createShowRemoveTaskDialog(idsToRemove, id))

    override fun onRemoveWithSubTasksChange() = run(intentOnRemoveWithSubTasksChange())

    override fun showSelectUrlDialog(urls: List<String>) = run(intentShowSelectUrlDialog(urls))
}

interface WithDialog<ID_TYPE, T> {

    val dialog: DialogState<ID_TYPE>

    fun copyWithDialog(dialog: DialogState<ID_TYPE>): T
}

sealed class DialogState<ID_TYPE> {

    data class RemoveDialog<ID>(
        val idsToRemove: List<ID>, val id: String = ""
    ) : DialogState<ID>()

    data class RemoveDialogWithCheckBox<ID>(
        val idsToRemove: List<ID>, val checked: Boolean = false, val id:String = ""
    ) : DialogState<ID>()

    data class InputDialog<ID>(
        val inputField: String,
    ) : DialogState<ID>()

    data class SelectOptionsDialog<ID>(
        val items: List<Any>,
    ) : DialogState<ID>()

    data class NoDialog<ID>(val unit: Unit = Unit) : DialogState<ID>()
}
