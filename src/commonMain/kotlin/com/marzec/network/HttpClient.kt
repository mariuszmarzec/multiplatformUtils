package com.marzec.network

import com.marzec.cache.FileCache
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun createHttpClient(
    fileCache: FileCache,
    authorizationHeader: String,
    preferencesHeaderKey: String,
    retryOnConnectionFailure: Boolean = false,
    block: HttpClientConfig<*>.() -> Unit = {}
): HttpClient