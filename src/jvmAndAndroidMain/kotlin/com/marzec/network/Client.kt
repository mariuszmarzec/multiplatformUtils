package com.marzec.network

import com.marzec.cache.FileCache
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createHttpClient(
    fileCache: FileCache,
    authorizationHeader: String,
    preferencesHeaderKey: String,
    retryOnConnectionFailure: Boolean,
    block: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }

    install(AuthPlugin) {
        this.fileCache = fileCache
        this.authorizationHeader = authorizationHeader
        this.preferencesHeaderKey = preferencesHeaderKey
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    engine {
        config {
            if (retryOnConnectionFailure) {
                retryOnConnectionFailure(true)
            }
        }
//        addNetworkInterceptor(HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        })
    }
    block()
}
