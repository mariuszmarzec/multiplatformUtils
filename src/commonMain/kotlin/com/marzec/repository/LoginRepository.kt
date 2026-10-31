package com.marzec.repository

import com.marzec.api.LoginRequestDto
import com.marzec.api.UserDto
import com.marzec.api.toDomain
import com.marzec.cache.FileCache
import com.marzec.cache.putTyped
import com.marzec.cache.getTyped
import com.marzec.cache.observeTyped
import com.marzec.model.User
import com.marzec.content.Content
import com.marzec.content.asContentFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

interface LoginRepository {

    suspend fun login(login: String, password: String): Flow<Content<User>>
    suspend fun logout(): Flow<Content<Unit>>
    suspend fun getCurrentUser(): User?
    suspend fun observeCurrentUser(): Flow<Content<User>>
}

class LoginRepositoryMock : LoginRepository {
    override suspend fun login(login: String, password: String): Flow<Content<User>> =
        flowOf(Content.Data(User(1, "mock@user.com")))

    override suspend fun logout(): Flow<Content<Unit>> = flowOf(Content.Data(Unit))

    override suspend fun getCurrentUser(): User? = User(1, "mock@user.com")
    override suspend fun observeCurrentUser(): Flow<Content<User>> {
        return flowOf(Content.Data(User(1, "mock@user.com")))
    }
}

class LoginRepositoryImpl(
    private val client: HttpClient,
    private val dispatcher: CoroutineDispatcher,
    private val fileCache: FileCache,
    private val loginApiUrl: String,
    private val logoutApiUrl: String,
    private val authorizationPreferencesKey: String,
    private val userCacheKey: String = "current_user",
) : LoginRepository {

    override suspend fun login(login: String, password: String): Flow<Content<User>> =
        asContentFlow {
            val userDto = client.post(loginApiUrl) {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(login, password))
            }.body<UserDto>()
            fileCache.putTyped(userCacheKey, userDto)
            userDto.toDomain()
        }.flowOn(dispatcher)

    override suspend fun logout(): Flow<Content<Unit>> =
        asContentFlow {
            fileCache.putTyped<String>(authorizationPreferencesKey, null)
            fileCache.putTyped<User>(userCacheKey, null)
            client.get(logoutApiUrl).body<Unit>()
        }.flowOn(dispatcher)

    override suspend fun getCurrentUser(): User? =
        fileCache.getTyped<UserDto>(userCacheKey)?.toDomain()

    override suspend fun observeCurrentUser(): Flow<Content<User>> {
        return fileCache.observeTyped<UserDto>(userCacheKey).map {
            it?.let { Content.Data(it.toDomain()) } ?: Content.Error(
                IllegalStateException("current user not found")
            )
        }.onStart { emit(Content.Loading()) }
    }
}
