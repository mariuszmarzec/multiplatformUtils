package com.marzec.featuretoggle

import kotlinx.coroutines.flow.Flow

interface FeatureTogglesManager {

    fun observe(featureToggle: String): Flow<Boolean>

    fun get(featureToggle: String): Boolean
}

interface FeatureTogglesManagerInitializer {

    fun create(): FeatureTogglesManager
}
