package com.marzec.preferences

interface StateCache {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}

class MemoryStateCache : StateCache {

    private val map = HashMap<String, Any>()

    override fun set(key: String, value: Any) {
        map[key] = value
    }

    @Suppress("unchecked_cast")
    override fun <T> get(key: String): T? = map[key] as? T

    override fun remove(key: String) {
        map.remove(key)
    }
}
