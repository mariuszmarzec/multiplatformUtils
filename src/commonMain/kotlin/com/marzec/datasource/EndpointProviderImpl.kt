package com.marzec.datasource

class EndpointProviderImpl<ID>(private val baseEndpoint: String) : EndpointProvider<ID> {

    override fun getAll(): String = baseEndpoint

    override fun getById(id: ID): String = byIdEndpoint(id)

    override fun create() = baseEndpoint

    override fun update(id: ID) = byIdEndpoint(id)

    override fun remove(id: ID) = byIdEndpoint(id)

    private fun byIdEndpoint(id: ID) = "$baseEndpoint/$id"
}