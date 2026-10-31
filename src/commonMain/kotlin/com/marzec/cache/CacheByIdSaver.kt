package com.marzec.cache

import kotlinx.coroutines.flow.Flow

interface CacheByIdSaver<ID, MODEL> {

    suspend fun getById(id: ID): MODEL?

    suspend fun observeCachedById(id: ID): Flow<MODEL?>

    suspend fun updateItem(id: ID, data: MODEL)

    suspend fun addItem(data: MODEL)

    suspend fun removeItem(id: ID)
}
