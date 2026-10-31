package com.marzec.view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageDecodeTest {

    @Test
    fun `decodeImageSafely handles corrupt bytes gracefully with Failure`() {
        val corruptImage = Image(bytes = byteArrayOf(0x00, 0x01, 0x02), extension = "png")
        val result = decodeImageSafely(corruptImage, animationEnabled = false)
        assertEquals(DecodedImage.Failure, result)
    }

    @Test
    fun `decodeImageSafely handles corrupt animated image gracefully with Failure`() {
        val corruptGif = Image(bytes = byteArrayOf(0x00, 0x01, 0x02), extension = "gif")
        val result = decodeImageSafely(corruptGif, animationEnabled = true)
        assertEquals(DecodedImage.Failure, result)
    }
}
