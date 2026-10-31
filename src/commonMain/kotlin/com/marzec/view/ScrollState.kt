package com.marzec.view

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import com.marzec.cache.MemoryCache
import com.marzec.navigation.NavigationCache
import kotlinx.coroutines.runBlocking

val LocalScrollStateMap = staticCompositionLocalOf<NavigationCache> { NavigationCacheProxy(MemoryCache()) }
val LocalScrollListStateMap = staticCompositionLocalOf<NavigationCache> { NavigationCacheProxy(MemoryCache()) }

data class Scroll(
    val value: Int
)

data class ScrollListState(
    val index: Int = 0,
    val offset: Int = 0
)

@Composable
fun rememberForeverScrollState(
    key: Any,
    initial: Int = 0
): ScrollState {
    val map = LocalScrollStateMap.current
    val scrollState = rememberSaveable(saver = ScrollState.Saver) {
        runBlocking {
            val scrollValue: Int = map.get<Scroll>(key.toString())?.value ?: initial
            map.put(key.toString(), Scroll(scrollValue))
            ScrollState(scrollValue)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runBlocking {
                map.put(key.toString(), Scroll(scrollState.value))
            }
        }
    }
    return scrollState
}

@Composable
fun rememberForeverListState(
    key: Any,
    index: Int = 0,
    offset: Int = 0
): LazyListState {
    val map = LocalScrollListStateMap.current
    val scrollState = rememberSaveable(saver = LazyListState.Saver) {
        runBlocking {
            val scrollValue = map.get<ScrollListState>(key.toString())
                ?: ScrollListState(index, offset)
            map.put(key.toString(), scrollValue)
            LazyListState(scrollValue.index, scrollValue.offset)
        }
    }
    DisposableEffect(key) {
        onDispose {
            runBlocking {
                map.put(
                    key.toString(),
                    ScrollListState(
                        scrollState.firstVisibleItemIndex,
                        scrollState.firstVisibleItemScrollOffset
                    )
                )
            }
        }
    }
    return scrollState
}