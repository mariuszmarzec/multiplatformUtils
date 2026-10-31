package com.marzec.decimal

import java.math.BigDecimal

actual class Decimal actual constructor(value: String) {

    private val value: BigDecimal

    init {
        this.value = BigDecimal(value)
    }

    actual operator fun plus(other: Decimal): Decimal = Decimal((value + other.value).toString())

    actual operator fun minus(other: Decimal): Decimal = Decimal((value - other.value).toString())

    actual operator fun times(other: Decimal): Decimal = Decimal((value * other.value).toString())

    actual operator fun div(other: Decimal): Decimal = Decimal((value / other.value).toString())

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = value == other
}