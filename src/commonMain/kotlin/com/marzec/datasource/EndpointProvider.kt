package com.marzec.datasource

interface EndpointProvider<ID> {

    fun getAll(): String

    fun getById(id: ID): String

    fun create(): String

    fun update(id: ID): String

    fun remove(id: ID): String
}