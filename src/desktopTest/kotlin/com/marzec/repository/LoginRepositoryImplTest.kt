package com.marzec.repository

import com.marzec.api.UserDto
import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import com.marzec.content.Content
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginRepositoryImplTest {

    @Test
    fun logoutClearsCachedUserUsingTheDtoSerializer() = runTest {
        val fileCache = InMemoryFileCache()
        fileCache.putTyped("AUTHORIZATION", "test-token")
        fileCache.putTyped("current_user", UserDto(1, "test@example.com"))
        val client = HttpClient(MockEngine { request ->
            assertEquals("/logout", request.url.encodedPath)
            respond(
                content = ByteReadChannel.Empty,
                status = HttpStatusCode.NoContent
            )
        })
        val repository = LoginRepositoryImpl(
            client = client,
            dispatcher = Dispatchers.Unconfined,
            fileCache = fileCache,
            loginApiUrl = "https://example.test/login",
            logoutApiUrl = "https://example.test/logout",
            authorizationPreferencesKey = "AUTHORIZATION"
        )

        try {
            val result = repository.logout().first { it !is Content.Loading }

            assertEquals(Content.Data(Unit), result)
            assertNull(fileCache.getTyped<UserDto>("current_user"))
            assertNull(fileCache.getTyped<String>("AUTHORIZATION"))
        } finally {
            client.close()
        }
    }
}

private class InMemoryFileCache : FileCache {
    private val json = Json { ignoreUnknownKeys = true }
    private val values = mutableMapOf<String, JsonElement>()

    override suspend fun <T : Any> put(key: String, value: T?, serializer: KSerializer<T>) {
        values[key] = value?.let { json.encodeToJsonElement(serializer, it) } ?: JsonNull
    }

    override suspend fun <T : Any> get(key: String, serializer: KSerializer<T>): T? =
        when (val value = values[key]) {
            null, JsonNull -> null
            else -> json.decodeFromJsonElement(serializer, value)
        }

    override suspend fun <T : Any> observe(key: String, serializer: KSerializer<T>): Flow<T?> =
        flowOf(get(key, serializer))

    override suspend fun <T : Any> update(
        key: String,
        update: (T?) -> T?,
        serializer: KSerializer<T>
    ) {
        put(key, update(get(key, serializer)), serializer)
    }
}
