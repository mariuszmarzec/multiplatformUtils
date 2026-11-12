package com.marzec.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtensionsTest {

    @Test
    fun `markdown link destination excludes closing delimiter`() {
        assertEquals(
            listOf("https://google.com"),
            "[Google](https://google.com)".urls()
        )
    }

    @Test
    fun `plain URL is unchanged`() {
        assertEquals(
            listOf("https://google.com"),
            "https://google.com".urls()
        )
    }

    @Test
    fun `balanced parentheses inside URL are preserved`() {
        assertEquals(
            listOf("https://example.com/path_(mathematics)"),
            "https://example.com/path_(mathematics)".urls()
        )
    }

    @Test
    fun `markdown delimiter is removed but balanced URL parentheses are preserved`() {
        assertEquals(
            listOf("https://example.com/path_(mathematics)"),
            "[Math](https://example.com/path_(mathematics))".urls()
        )
    }

    @Test
    fun `multiple markdown links are extracted without delimiters`() {
        assertEquals(
            listOf("https://google.com", "https://openai.com"),
            "[Google](https://google.com) and [OpenAI](https://openai.com)".urls()
        )
    }
}
