package com.marzec.featuretoggle

import com.marzec.cache.Cache
import com.marzec.content.ifDataSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FeatureTogglesManagerImpl(
    private val featureTogglesDefaults: Map<String, String>,
    private val featureToggleRepository: FeatureToggleRepository,
    private val memoryCache: Cache,
    private val updaterCoroutineScope: CoroutineScope
) : FeatureTogglesManager {

    fun init() {
        runBlocking {
            featureTogglesDefaults.forEach {
                memoryCache.put(it.key, it.value)
            }
        }
        update()
    }

    private fun update() {
        updaterCoroutineScope.launch {
            featureToggleRepository.observeAll().collect { featureToggles ->
                featureToggles.ifDataSuspend {
                    data.forEach {
                        memoryCache.put(it.name, it.value)
                    }
                }
            }
        }
    }

    override fun observe(featureToggle: String): Flow<Boolean> = runBlocking {
        memoryCache.observe<String>(featureToggle)
            .map { it.toBoolean() }
            .also { triggerUpdate() }
    }

    private fun triggerUpdate() {
        updaterCoroutineScope.launch {
            featureToggleRepository.refreshAll()
        }
    }

    override fun get(featureToggle: String): Boolean = runBlocking {
        try {
            memoryCache.get<String>(featureToggle).toBoolean()
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            false
        }
    }
}
