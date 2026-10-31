package com.marzec.repository

/**
 * Controls the value returned by an explicit refresh that fails.
 *
 * This is not a TTL policy. Cached models are never deleted by a failed refresh.
 */
enum class CacheExpirationMode {
    AlwaysExpired,
    AlwaysUpToDate
}
