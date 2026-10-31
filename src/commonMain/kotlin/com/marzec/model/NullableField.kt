package com.marzec.model

import com.marzec.dto.NullableFieldDto

data class NullableField<T>(val value: T?)

fun <T> NullableField<T>.toDto(): NullableFieldDto<T> = NullableFieldDto(value)

fun <T, R> NullableField<T>.toDto(valueMapper: (T?)-> R?): NullableFieldDto<R> =
    NullableFieldDto(valueMapper(this.value))

fun <T : Any> T.toUpdate(old: T): T? = takeIf { old != this }

fun <T : Any> T?.toNullableUpdate(old: T?): NullableField<T>? =
    NullableField(this).takeIf { old != this }