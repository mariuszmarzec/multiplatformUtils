package com.marzec.cache

import kotlinx.serialization.KSerializer

@Suppress("FunctionName")
fun <ID, MODEL> FileCacheListCacheSaver(
    key: String,
    fileCache: FileCache,
    isSameId: MODEL.(id: ID) -> Boolean,
    serializer: KSerializer<List<MODEL>>,
    newItemInsert: List<MODEL>?.(item: MODEL) -> List<MODEL>? = atFirstPositionInserter()
) = ListCacheSaverImpl(
    cacheSaver = FileCacheSaver(
        key = key,
        fileCache = fileCache,
        serializer = serializer
    ),
    isSameId = isSameId,
    newItemInsert = newItemInsert
)
