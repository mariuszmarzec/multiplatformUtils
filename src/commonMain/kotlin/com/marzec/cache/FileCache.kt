package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

interface FileCache {

    suspend fun <T: Any> put(key: String, value: T?, serializer: KSerializer<T>)

    suspend fun <T: Any> get(key: String, serializer: KSerializer<T>): T?

    suspend fun <T: Any> observe(key: String, serializer: KSerializer<T>): Flow<T?>

    suspend fun <T: Any> update(key: String, update: (T?) -> T?, serializer: KSerializer<T>)
}

suspend inline fun <reified T: Any> FileCache.putTyped(key: String, value: T?) {
    put(key, value, serializer(typeOf<T>()) as KSerializer<T>)
}

suspend inline fun <reified T: Any> FileCache.getTyped(key: String): T? {
    return get(key, serializer(typeOf<T>()) as KSerializer<T>)
}

suspend inline fun <reified T: Any> FileCache.observeTyped(key: String): Flow<T?> {
    return observe(key, serializer(typeOf<T>()) as KSerializer<T>)
}
