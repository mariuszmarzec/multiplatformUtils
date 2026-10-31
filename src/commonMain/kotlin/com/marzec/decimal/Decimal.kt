package com.marzec.decimal

expect class Decimal(value: String) {

    operator fun plus(other: Decimal): Decimal

    operator fun minus(other: Decimal): Decimal

    operator fun times(other: Decimal): Decimal

    operator fun div(other: Decimal): Decimal

}

fun String.toDecimal(): Decimal = Decimal(this)