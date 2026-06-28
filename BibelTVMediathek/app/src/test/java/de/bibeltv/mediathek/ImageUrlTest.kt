package de.bibeltv.mediathek

import de.bibeltv.mediathek.data.mapper.imageUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUrlTest {
    @Test
    fun absoluteUrlUnchanged() {
        assertEquals("https://x.de/a.jpg", imageUrl("https://x.de/a.jpg"))
    }

    @Test
    fun relativePathGetsImgixPrefix() {
        val u = imageUrl("folder/a.jpg")
        assertTrue(u, u!!.startsWith("https://bibeltv.imgix.net/folder/a.jpg"))
    }

    @Test
    fun nullOrBlankReturnsNull() {
        assertNull(imageUrl(null))
        assertNull(imageUrl(""))
        assertNull(imageUrl("   "))
    }
}
