package com.marzec.repository

/**
 * Controls whether a paging request loads one page or completes the collection.
 */
sealed interface Paging {
    data object OnDemand : Paging
    data object Eagerly : Paging
}
