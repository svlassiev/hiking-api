package info.vlassiev.serg.image

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Synthetic "photos": smooth gradients (sky, light) plus fine noise (foliage, gravel), so the JPEG
 * is about as large and as slow to decode as a phone photo of a forest — not a flat test card.
 */
object TestPhotos {
    fun jpeg(width: Int, height: Int, gray: Boolean = false, quality: Float = 0.92f): File {
        val type = if (gray) BufferedImage.TYPE_BYTE_GRAY else BufferedImage.TYPE_3BYTE_BGR
        var image: BufferedImage? = BufferedImage(width, height, type)
        val raster = image!!.raster
        var seed = 0x2545F491L
        val row = IntArray(width * raster.numBands)
        for (y in 0 until height) {
            var i = 0
            for (x in 0 until width) {
                seed = seed xor (seed shl 13); seed = seed xor (seed ushr 7); seed = seed xor (seed shl 17)
                val noise = (seed and 0x3F).toInt() - 32
                val base = (x * 255 / width + y * 255 / height) / 2
                for (b in 0 until raster.numBands) row[i++] = (base + noise + b * 20).coerceIn(0, 255)
            }
            raster.setPixels(0, y, width, 1, row)
        }
        val file = File.createTempFile("photo-${width}x$height-", ".jpg").apply { deleteOnExit() }
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        ImageIO.createImageOutputStream(file).use { out ->
            writer.output = out
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality
            }
            writer.write(null, IIOImage(image, null, null), param)
        }
        writer.dispose()
        image = null // the caller measures memory: let this go before it starts
        System.gc()
        return file
    }
}
