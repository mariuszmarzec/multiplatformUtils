package com.marzec.mvi

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend

fun <State : Any, Result> IntentBuilder<State, Content<Result>>.postSideEffect(
    func: suspend IntentContext<State, Content<Result>>.() -> Unit
) {
    cancelTrigger(runSideEffectAfterCancel = true) {
        resultNonNull() is Content.Data
    }

    sideEffect {
        resultNonNull().ifDataSuspend { func() }
    }
}
