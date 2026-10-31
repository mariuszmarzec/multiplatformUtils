package com.marzec.network

import com.marzec.cache.FileCache
import com.marzec.cache.getTyped
import com.marzec.cache.putTyped
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header

private const val HTTP_STATUS_UNAUTHORIZED = 401

class AuthPluginConfig {
    lateinit var fileCache: FileCache
    lateinit var authorizationHeader: String
    lateinit var preferencesHeaderKey: String
}

val AuthPlugin: ClientPlugin<AuthPluginConfig> = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val fileCache = pluginConfig.fileCache
    val authorizationHeader = pluginConfig.authorizationHeader
    val preferencesHeaderKey = pluginConfig.preferencesHeaderKey

    onRequest { request, _ ->
        fileCache.getTyped<String>(preferencesHeaderKey)?.let { authToken ->
            request.header(authorizationHeader, authToken)
        }
    }

    onResponse { response ->
        response.headers[authorizationHeader]?.let { newAuthToken ->
            fileCache.putTyped(preferencesHeaderKey, newAuthToken)
        } ?: run {
            if (response.status.value == HTTP_STATUS_UNAUTHORIZED) {
                fileCache.putTyped<String>(preferencesHeaderKey, null)
            }
        }
    }
}
