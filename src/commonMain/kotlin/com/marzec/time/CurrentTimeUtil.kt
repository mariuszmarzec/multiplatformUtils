package com.marzec.time

import java.time.Clock
import java.time.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime

const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
const val SHORT_DATE_FORMAT = "yyyy-MM-dd"

object CurrentTimeUtil {

    internal var clock: () -> Clock = { Clock.systemDefaultZone() }

    fun setOtherTime(day: Int, month: Int, year: Int) {
        val instant =
            LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant()
        clock = { Clock.fixed(instant, ZoneId.systemDefault()) }
    }
}

fun currentTime(): LocalDateTime =
    java.time.LocalDateTime.now(CurrentTimeUtil.clock()).toKotlinLocalDateTime()

fun LocalDateTime.withStartOfDay(): LocalDateTime =
    toJavaLocalDateTime()
        .withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .toKotlinLocalDateTime()

fun currentMillis(): Long =
    java.time.LocalDateTime.now(CurrentTimeUtil.clock()).toInstant(OffsetDateTime.now().offset)
        .toEpochMilli()

fun LocalDateTime.formatDate(dateFormat: String = DEFAULT_DATE_FORMAT): String =
    toJavaLocalDateTime().format(DateTimeFormatter.ofPattern(dateFormat))

fun String.shortDateToLocalDateTime() = "${this}T00:00:00".toLocalDateTime()

fun time(day: Int, month: Int, year: Int, hour: Int = 0, minute: Int = 0): LocalDateTime =
    java.time.LocalDateTime.of(year, month, day, hour, minute).toKotlinLocalDateTime()

fun LocalDateTime.lengthOfMonth(): Int = YearMonth.of(year, monthNumber).lengthOfMonth()

fun LocalDateTime.plusDays(days: Int): LocalDateTime =
    toJavaLocalDateTime().plusDays(days.toLong()).toKotlinLocalDateTime()