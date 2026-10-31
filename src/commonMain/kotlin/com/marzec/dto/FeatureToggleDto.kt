package com.marzec.dto

import com.marzec.model.FeatureToggle
import com.marzec.model.NewFeatureToggle
import kotlinx.serialization.Serializable

@Serializable
data class FeatureToggleDto(
    val id: Int,
    val name: String,
    val value: String
)

@Serializable
data class NewFeatureToggleDto(val name: String, val value:String)

@Serializable
data class UpdateFeatureToggleDto(
    val name: String?,
    val value: String?
)

fun FeatureToggleDto.toDomain() = FeatureToggle(id, name, value)

fun NewFeatureToggleDto.toDomain() = NewFeatureToggle(name, value)