package info.vlassiev.serg.image

import info.vlassiev.serg.model.VariantName
import info.vlassiev.serg.model.VariantName.*
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The resized copies of an uploaded original, largest first: each is made from the one before it.
 *
 * This replaced a version that decoded the full original once PER variant and scaled it with
 * ULTRA_QUALITY (1/7-step incremental scaling, many near-full-size intermediates). On the
 * production pod — a 126 MB heap and 0.2 CPU — that took 25-45 s per phone photo, and on
 * 2026-09-18 12 of 20 registrations died with OutOfMemoryError half-way through.
 */
internal val VARIANT_SIZES = listOf(V2048 to 2048, V1024 to 1024, V800 to 800, THUMBNAIL to 80)

/**
 * Most pixels ever decoded at once: 12.6 MP ≈ 50 MB as an int-RGB raster. A normal 12.5 MP
 * phone photo (4080×3072) is decoded in full; anything bigger is thinned out while decoding.
 */
internal const val MAX_DECODED_PIXELS = 12_600_000L

/**
 * The integer step the JPEG decoder skips pixels by, so that the decoded image holds at most
 * [MAX_DECODED_PIXELS] and its long side is still at least [largest] (2048). 1 = full decode.
 */
internal fun decodeStep(width: Int, height: Int, largest: Int = VARIANT_SIZES.first().second): Int {
    val pixels = width.toLong() * height
    val byMemory = ceil(sqrt(pixels.toDouble() / MAX_DECODED_PIXELS)).toInt()
    val byQuality = max(1, max(width, height) / largest)
    return max(1, minOf(byMemory, byQuality))
}

/**
 * Decodes [file] ONCE, straight into an int-RGB raster (imgscalr's working format — otherwise it
 * makes a second full-size copy first), thinned out by [decodeStep] when the photo is huge.
 */
internal fun decodeForVariants(file: File): BufferedImage {
    val input = ImageIO.createImageInputStream(file) ?: throw IOException("Cannot open $file")
    input.use {
        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull()
            ?: throw IOException("Not an image: $file")
        try {
            reader.setInput(input, true, true) // metadata is read elsewhere, from the file
            val param = reader.defaultReadParam
            val step = decodeStep(reader.getWidth(0), reader.getHeight(0))
            if (step > 1) param.setSourceSubsampling(step, step, 0, 0)
            reader.getImageTypes(0).asSequence()
                .firstOrNull { it.bufferedImageType == BufferedImage.TYPE_INT_RGB }
                ?.let { param.destinationType = it }
            return reader.read(0, param)
        } finally {
            reader.dispose()
        }
    }
}

/**
 * Writes every variant of [original] to a temp JPEG and hands each to [consume] as soon as it
 * exists (to upload it), then deletes it. Never holds more than two images: the current source
 * and the variant being made from it. Failures throw — they are not swallowed into a missing file.
 */
internal fun makeVariants(original: File, consume: (VariantName, File) -> Unit) {
    var source = decodeForVariants(original)
    try {
        for ((name, size) in VARIANT_SIZES) {
            val variant = Scalr.resize(source, Scalr.Method.QUALITY, size)
            val out = Files.createTempFile("", ".jpg").toFile()
            try {
                if (!ImageIO.write(variant, "JPEG", out)) throw IOException("No JPEG writer")
                consume(name, out)
            } finally {
                out.delete()
            }
            // Only a smaller image becomes the next source. A photo under 2048 px is still
            // enlarged to 2048 as before, but the smaller sizes are then made from the original,
            // not from its enlargement.
            if (max(variant.width, variant.height) < max(source.width, source.height)) {
                source.flush()
                source = variant
            } else {
                variant.flush()
            }
        }
    } finally {
        source.flush()
    }
}
