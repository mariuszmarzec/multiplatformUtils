package com.marzec.extensions

const val EMPTY_STRING = ""

@Suppress("unchecked_cast")
inline fun <reified T: Any> Any.asInstance(action: T.() -> Unit) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T: Any, R> Any.asInstanceAndReturnOther(action: T.() -> R) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T: Any> Any.asInstanceAndReturn(action: T.() -> T) = (this as? T)?.action()

inline fun <T> Boolean.ifTrue(valueLambda: () -> T): T? = if (this) valueLambda() else null

inline fun <T> Boolean.ifFalse(valueLambda: () -> T): T? = if (this) null else valueLambda()
