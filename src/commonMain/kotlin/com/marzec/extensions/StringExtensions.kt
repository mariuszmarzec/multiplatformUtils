package com.marzec.extensions

import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.urls(): List<String> {
    val containedUrls: MutableList<String> = ArrayList()
    val urlRegex =
        "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)"
    val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE)
    val urlMatcher: Matcher = pattern.matcher(this)

    while (urlMatcher.find()) {
        val url = substring(urlMatcher.start(0), urlMatcher.end(0))
            .trimUnmatchedTrailingParentheses()
        if (url.isNotEmpty()) {
            containedUrls.add(url)
        }
    }

    return containedUrls
}

private fun String.trimUnmatchedTrailingParentheses(): String {
    var openingParentheses = count { it == '(' }
    var closingParentheses = count { it == ')' }
    var endIndex = length

    while (
        endIndex > 0 &&
        this[endIndex - 1] == ')' &&
        closingParentheses > openingParentheses
    ) {
        endIndex--
        closingParentheses--
    }

    return substring(0, endIndex)
}

fun String.nullIfEmpty() = ifEmpty { null }
