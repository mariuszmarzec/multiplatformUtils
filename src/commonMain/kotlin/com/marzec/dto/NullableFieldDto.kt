package com.marzec.dto

import kotlinx.serialization.Serializable

@Serializable
data class NullableFieldDto<T>(val value: T?)
