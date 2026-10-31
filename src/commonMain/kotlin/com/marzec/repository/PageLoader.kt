package com.marzec.repository

/** Loads one raw page from the remote source. */
fun interface PageLoader<CURSOR, MODEL> {
    suspend fun load(cursor: CURSOR?): Page<CURSOR, MODEL>
}
