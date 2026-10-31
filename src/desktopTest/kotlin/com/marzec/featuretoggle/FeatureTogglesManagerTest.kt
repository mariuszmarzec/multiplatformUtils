package com.marzec.featuretoggle

import com.marzec.cache.Cache
import com.marzec.cache.MemoryCache
import com.marzec.content.Content
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureTogglesManagerTest {

    private val scope = CoroutineScope(SupervisorJob())
    private val repository = mockk<FeatureToggleRepository>(relaxed = true)

    init {
        coEvery { repository.observeAll() } returns flowOf(Content.Data(emptyList()))
    }

    private fun newManager(defaults: Map<String, String>): FeatureTogglesManager {
        val cache = MemoryCache()
        return FeatureTogglesManagerImpl(
            featureTogglesDefaults = defaults,
            featureToggleRepository = repository,
            memoryCache = cache,
            updaterCoroutineScope = scope
        ).apply { init() }
    }

    @Test
    fun `get returns true when default is true`() = runTest {
        val manager = newManager(mapOf("fiteo.test" to "true"))
        assertTrue(manager.get("fiteo.test"))
    }

    @Test
    fun `get returns false when default is false`() = runTest {
        val manager = newManager(mapOf("fiteo.test" to "false"))
        assertFalse(manager.get("fiteo.test"))
    }

    @Test
    fun `get returns false when toggle is missing from defaults`() = runTest {
        val manager = newManager(mapOf("fiteo.other" to "true"))
        assertFalse(manager.get("fiteo.missing"))
    }

    @Test
    fun `get returns false when default value is malformed`() = runTest {
        val manager = newManager(mapOf("fiteo.test" to "yes"))
        assertFalse(manager.get("fiteo.test"))
    }

    @Test
    fun `get returns false when cache access throws cancellation while coroutine is active`() = runTest {
        val cache = mockk<Cache>()
        coEvery { cache.get<String>(any()) } throws CancellationException("cancelled")
        val manager = FeatureTogglesManagerImpl(
            featureTogglesDefaults = emptyMap(),
            featureToggleRepository = mockk(relaxed = true),
            memoryCache = cache,
            updaterCoroutineScope = scope
        )
        assertFalse(manager.get("fiteo.test"))
    }
}
