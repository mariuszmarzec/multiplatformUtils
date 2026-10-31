package com.marzec.preferences

interface Preferences {
    fun set(key: String, value: Any)
    fun getString(key: String): String?
    fun <T> get(key: String): T?
    fun remove(key: String)
}

class MemoryPreferences : Preferences {

    private val map = HashMap<String, Any>()

    override fun set(key: String, value: Any) {
        map[key] = value
    }

    override fun getString(key: String): String? = map[key] as? String

    @Suppress("unchecked_cast")
    override fun <T> get(key: String): T? = map[key] as? T

    override fun remove(key: String) {
        map.remove(key)
    }
}
