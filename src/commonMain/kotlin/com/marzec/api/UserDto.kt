package com.marzec.api

import com.marzec.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: Int, val email: String)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

fun UserDto.toDomain(): User = User(id, email)
