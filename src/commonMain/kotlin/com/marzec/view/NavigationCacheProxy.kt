package com.marzec.view

import com.marzec.cache.Cache
import com.marzec.navigation.NavigationCache
import kotlinx.coroutines.flow.Flow

/**
 * Adapts [Cache] to the navigation-owned [NavigationCache] contract.
 *
 * Used by library-internal wiring ([NavigationHost] factories and the
 * [LocalScrollStateMap] / [LocalScrollListStateMap] defaults). Clients may
 * provide their own proxy instead.
 */
class NavigationCacheProxy(private val cache: Cache) : NavigationCache {

    override suspend fun put(key: String, value: Any?) = cache.put(key, value)

    override suspend fun <T> get(key: String): T? = cache.get(key)

    override suspend fun remove(key: String) = cache.remove(key)

    override suspend fun <T> observe(key: String): Flow<T?> = cache.observe(key)

    override suspend fun toMap(): Map<String, Any?> = cache.toMap()
}
