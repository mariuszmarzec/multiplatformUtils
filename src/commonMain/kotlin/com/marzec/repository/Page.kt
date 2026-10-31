package com.marzec.repository

data class Page<CURSOR, MODEL>(
    val items: List<MODEL>,
    val nextCursor: CURSOR?
)
