package com.marzec.screen.featuretoggle

import com.marzec.delegate.DialogState
import com.marzec.delegate.WithDialog
import com.marzec.model.FeatureToggle
import com.marzec.mvi.State

data class FeatureToggleState(
    val id: Int?,
    val name: String,
    val value: String,
    val toggle: FeatureToggle? = null,
    override val dialog: DialogState<Int> = DialogState.NoDialog()
): WithDialog<Int, FeatureToggleState> {

    companion object {

        fun new(
            id: Int? = null,
            name: String = "",
            value: String = ""
        ) = FeatureToggleState(
            id = id,
            name = name,
            value = value,
        )

        fun initial(
            id: Int? = null,
            name: String = "",
            value: String = ""
        ): State<FeatureToggleState> = id?.let {
            State.Loading(
                new(
                    id = id,
                    name = name,
                    value = value
                )
            )
        } ?: State.Data(
            new(
                name = name,
                value = value
            )
        )
    }

    override fun copyWithDialog(dialog: DialogState<Int>) = copy(dialog = dialog)
}
