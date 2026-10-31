package com.marzec.screen.pickitemscreen

import com.marzec.content.Content
import com.marzec.mvi.State
import com.marzec.view.SearchState
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PickItemDataStoreTest {

    private fun options(supportsPaging: Boolean = false) = PickItemOptions(
        loadData = { flowOf<Content<List<Int>>>(Content.Data(emptyList())) },
        mapItemToId = { it.toString() },
        itemRow = { _, _ -> },
        supportsPaging = supportsPaging
    )

    private fun state(
        options: PickItemOptions<Int>,
        items: List<Int> = emptyList(),
        isComplete: Boolean = true
    ): State<PickItemData<Int>> = State.Data(
        PickItemData(
            options = options,
            items = items,
            selected = emptySet(),
            search = SearchState.DEFAULT,
            isComplete = isComplete
        )
    )

    @Test
    fun `legacy picker ignores partial loading payload and only replaces items on Data`() {
        val options = options()
        val initial = state(options, items = listOf(99))

        val loading = reducePickItemContent(
            initial,
            Content.Loading(data = listOf(1, 2)),
            options
        )

        val loadingState = assertIs<State.Loading<PickItemData<Int>>>(loading)
        assertEquals(listOf(99), loadingState.data?.items)

        val loaded = reducePickItemContent(
            loading,
            Content.Data(listOf(1, 2)),
            options
        )

        val loadedState = assertIs<State.Data<PickItemData<Int>>>(loaded)
        assertEquals(listOf(1, 2), loadedState.data.items)
    }

    @Test
    fun `paging picker shows incomplete items and marks final Data complete`() {
        val options = options(supportsPaging = true)
        val initial = state(options)

        val partial = reducePickItemContent(
            initial,
            Content.Loading(data = listOf(10)),
            options
        )

        val partialState = assertIs<State.Data<PickItemData<Int>>>(partial)
        assertEquals(listOf(10), partialState.data.items)
        assertFalse(partialState.data.isComplete)
        assertEquals(null, partialState.data.error)

        val stillLoading = reducePickItemContent(
            partialState,
            Content.Loading(),
            options
        )

        val stillLoadingState = assertIs<State.Data<PickItemData<Int>>>(stillLoading)
        assertEquals(listOf(10), stillLoadingState.data.items)
        assertFalse(stillLoadingState.data.isComplete)

        val complete = reducePickItemContent(
            stillLoadingState,
            Content.Data(listOf(10, 5)),
            options
        )

        val completeState = assertIs<State.Data<PickItemData<Int>>>(complete)
        assertEquals(listOf(10, 5), completeState.data.items)
        assertTrue(completeState.data.isComplete)
        assertEquals(null, completeState.data.error)
    }

    @Test
    fun `paging picker keeps partial items and reports continuation failure`() {
        val options = options(supportsPaging = true)
        val partial = reducePickItemContent(
            state(options),
            Content.Loading(data = listOf(10)),
            options
        )

        val failed = reducePickItemContent(
            partial,
            Content.Error(IllegalStateException("network down")),
            options
        )

        val failedState = assertIs<State.Data<PickItemData<Int>>>(failed)
        assertEquals(listOf(10), failedState.data.items)
        assertFalse(failedState.data.isComplete)
        assertEquals("network down", failedState.data.error)
    }

    @Test
    fun `paging picker reports an error when no partial items are available`() {
        val options = options(supportsPaging = true)

        val failed = reducePickItemContent(
            state(options),
            Content.Error(IllegalStateException("network down")),
            options
        )

        val failedState = assertIs<State.Error<PickItemData<Int>>>(failed)
        assertEquals("network down", failedState.message)
        assertTrue(failedState.data?.items.isNullOrEmpty())
    }
}
