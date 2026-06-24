package com.cafeminsu.data.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MenuImageReaderTest {
    @Test
    fun menuImageDataEqualityComparesBytesByContent() {
        val a = MenuImageData(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", fileName = "a.jpg")
        val b = MenuImageData(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", fileName = "a.jpg")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun menuImageDataDiffersWhenBytesOrMetadataDiffer() {
        val base = MenuImageData(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", fileName = "a.jpg")

        assertNotEquals(base, base.copy(bytes = byteArrayOf(9)))
        assertNotEquals(base, base.copy(mimeType = "image/png"))
        assertNotEquals(base, base.copy(fileName = "b.jpg"))
    }
}
