package com.marzec.view

import com.marzec.navigation.NavigationStateCache
import com.marzec.preferences.StateCache

/**
 * Adapts [StateCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by client-side wiring in DI. Clients may provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {

    override fun <T> get(key: String): T? = stateCache.get(key)

    override fun set(key: String, value: Any) = stateCache.set(key, value)

    override fun remove(key: String) = stateCache.remove(key)
}
