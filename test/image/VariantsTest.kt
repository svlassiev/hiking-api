package info.vlassiev.serg.image

import info.vlassiev.serg.model.VariantName
import info.vlassiev.serg.model.VariantName.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max

/**
 * The test JVM runs with the production pod's heap (126 MB, see build.gradle): the old code threw
 * OutOfMemoryError on 12 of 20 phone photos with exactly that heap on 2026-09-18.
 */
class VariantsTest {

    @Test
    fun `a phone photo is decoded in full and a huge one is thinned out while decoding`() {
        assertEquals(1, decodeStep(4080, 3072))   // 12.5 MP, the usual phone photo
        assertEquals(1, decodeStep(1600, 1200))   // small
        assertEquals(1, decodeStep(8000, 1000))   // long panorama, 8 MP: fits whole
        assertEquals(2, decodeStep(9000, 1500))   // 13.5 MP: just over the budget, still 4500 px long
        assertEquals(2, decodeStep(5656, 4242))   // 24 MP
        assertEquals(2, decodeStep(8160, 6144))   // 50 MP mode
        assertEquals(4, decodeStep(16320, 12288)) // 200 MP mode
    }

    @Test
    fun `a 12 MP phone photo makes all four variants, in order, within the production heap`() {
        val photo = TestPhotos.jpeg(4080, 3072)
        val made = linkedMapOf<VariantName, Pair<Int, Int>>()

        makeVariants(photo) { name, file ->
            val image = ImageIO.read(file)
            made[name] = image.width to image.height
        }

        assertEquals(listOf(V2048, V1024, V800, THUMBNAIL), made.keys.toList())
        assertEquals(listOf(2048, 1024, 800, 80), made.values.map { max(it.first, it.second) })
        made.values.forEach { (w, h) -> assertAspect(4080, 3072, w, h) }
    }

    @Test
    fun `a 24 MP photo is decoded at half size and still makes a full 2048 variant`() {
        val photo = TestPhotos.jpeg(5656, 4242, gray = true) // gray: the fixture itself must fit this heap too

        val decoded = decodeForVariants(photo)

        assertEquals(2828, decoded.width)
        assertEquals(2121, decoded.height)
        decoded.flush()
        val sizes = mutableListOf<Int>()
        makeVariants(photo) { _, file -> sizes += ImageIO.read(file).let { max(it.width, it.height) } }
        assertEquals(listOf(2048, 1024, 800, 80), sizes)
    }

    @Test
    fun `a photo smaller than 2048 is still enlarged to 2048, as before`() {
        val photo = TestPhotos.jpeg(1600, 1200)
        val sizes = mutableListOf<Int>()

        makeVariants(photo) { _, file -> sizes += ImageIO.read(file).let { max(it.width, it.height) } }

        assertEquals(listOf(2048, 1024, 800, 80), sizes)
    }

    @Test
    fun `a file that is not an image fails loudly instead of producing a missing variant`() {
        val notAPhoto = createTempFile(suffix = ".jpg").apply { writeText("not a jpeg"); deleteOnExit() }

        val failed = runCatching { makeVariants(notAPhoto) { _, _ -> } }

        assertTrue(failed.isFailure)
    }

    private fun assertAspect(w0: Int, h0: Int, w: Int, h: Int) {
        val expected = h0.toDouble() / w0 * w
        assertTrue("$w x $h is not ${w0}x$h0 scaled", abs(expected - h) <= 1.0)
    }
}
