package com.marzec.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

inline fun <ID, reified DTO : Any, reified CREATE : Any, reified UPDATE : Any> CrudDataSource(
    endpointProvider: EndpointProvider<ID>,
    client: HttpClient,
    json: Json
) = CrudDataSourceImpl(
    dtoClass = DTO::class,
    createClass = CREATE::class,
    updateClass = UPDATE::class,
    endpointProvider = endpointProvider,
    client = client,
    json = json
)

@OptIn(InternalSerializationApi::class)
class CrudDataSourceImpl<ID, DTO : Any, CREATE : Any, UPDATE : Any>(
    private val dtoClass: KClass<DTO>,
    private val createClass: KClass<CREATE>,
    private val updateClass: KClass<UPDATE>,
    private val endpointProvider: EndpointProvider<ID>,
    private val client: HttpClient,
    private val json: Json
) : CrudDataSource<ID, DTO, CREATE, UPDATE> {

    override suspend fun getAll(): List<DTO> {
        val body = client.get(endpointProvider.getAll()).body<String>()
        return json.decodeFromString(ArraySerializer(dtoClass, dtoClass.serializer()), body)
            .toList()
    }

    override suspend fun getById(id: ID): DTO =
        client.get(endpointProvider.getById(id)).body<String>().parse()

    override suspend fun create(create: CREATE): DTO =
        client.post(endpointProvider.create()) {
            setBody(create, TypeInfo(createClass, createClass.java))
        }.body<String>().parse()

    override suspend fun update(id: ID, update: UPDATE): DTO =
        client.patch(endpointProvider.update(id)) {
            setBody(update, TypeInfo(updateClass, updateClass.java))
        }.bodyAsText().parse()

    override suspend fun remove(id: ID) {
        client.delete(endpointProvider.remove(id))
    }

    private fun String.parse() = json.decodeFromString(dtoClass.serializer(), this)
}