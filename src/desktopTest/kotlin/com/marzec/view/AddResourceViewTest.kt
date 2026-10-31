package com.marzec.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class AddResourceViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `Should display search attempts`() {
        var clicked = false
        compose.setContent {
            AddResourceView(
                showEmptyState = false,
                onClick = { clicked = true },
                onRemoveAllButtonClick = { },
                clearButtonContentDescription = "clear",
                label = "label",
                emptyLabel = "empty_label"
            )
        }

        compose.onNodeWithTag(AddResourceViewPageObject.emptyLabel).assertDoesNotExist()
        compose.onNodeWithTag(AddResourceViewPageObject.contentRow).performClick()

        assertTrue(clicked)
    }
}