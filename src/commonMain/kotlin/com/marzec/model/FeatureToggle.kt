package com.marzec.model

import com.marzec.dto.FeatureToggleDto
import com.marzec.dto.NewFeatureToggleDto
import com.marzec.dto.UpdateFeatureToggleDto
import kotlinx.serialization.Serializable

data class FeatureToggle(
    val id: Int,
    val name: String,
    val value: String
)

data class NewFeatureToggle(
    val name: String,
    val value: String
)

data class UpdateFeatureToggle(
    val name: String?,
    val value: String?
)

fun FeatureToggle.toDto() = FeatureToggleDto(
    id, name, value
)

fun NewFeatureToggle.toDto() = NewFeatureToggleDto(
    name, value
)

fun UpdateFeatureToggle.toDto() = UpdateFeatureToggleDto(
    name, value
)
