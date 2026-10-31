package com.marzec.featuretoggle

import com.marzec.cache.Cache
import com.marzec.resource.ResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

class FeatureTogglesManagerInitializerImpl(
    private val resourceLoader: ResourceLoader,
    private val json: Json,
    private val featureToggleRepository: FeatureToggleRepository,
    private val memoryCache: Cache,
    private val updaterCoroutineScope: CoroutineScope,
    private val featureToggleConfFile: String
) : FeatureTogglesManagerInitializer {

    override fun create(): FeatureTogglesManager {
        val featureTogglesDefaults: Map<String, String> = resourceLoader.loadComonResourceFile(featureToggleConfFile)
            ?.let {
                try {
                    json.decodeFromString<Map<String, String>>(it)
                } catch (_: Exception) {
                    null
                }
            }.orEmpty()
        return FeatureTogglesManagerImpl(
            featureTogglesDefaults = featureTogglesDefaults,
            featureToggleRepository = featureToggleRepository,
            memoryCache = memoryCache,
            updaterCoroutineScope = updaterCoroutineScope
        ).apply { init() }
    }
}
