package com.marzec.datasource

interface CrudDataSource<ID, DTO, CREATE, UPDATE> {

    suspend fun getAll(): List<DTO>

    suspend fun getById(id: ID): DTO

    suspend fun create(create: CREATE): DTO

    suspend fun update(id: ID, update: UPDATE): DTO

    suspend fun remove(id: ID)
}
