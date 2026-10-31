package com.marzec.uuid

import java.util.UUID

class UuidImpl : Uuid {
    override fun create(): String = UUID.randomUUID().toString()
}
