package info.vlassiev.serg.image

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Tag
import com.google.cloud.storage.BlobId
import info.vlassiev.serg.model.GpsData
import info.vlassiev.serg.model.Image
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private val logger = LoggerFactory.getLogger("MetadataExtractor")

private fun getImageGpsData(tags: Collection<Tag>): GpsData {
    val gps = GpsData()
    tags.forEach { tag -> when(tag.tagName) {
        "GPS Latitude Ref" -> gps.latitudeRef = tag.description
        "GPS Latitude" -> gps.latitude = tag.description
        "GPS Longitude Ref" -> gps.longitudeRef = tag.description
        "GPS Longitude" -> gps.longitude = tag.description
        "GPS Altitude Ref" -> gps.altitudeRef = tag.description
        "GPS Altitude" -> gps.altitude = tag.description
    } }
    return gps
}

private data class ImageMetadata(var originalTime: Long = 0, var userComment: String = "", var imageId: String = UUID.randomUUID().toString(), var gps: GpsData? = null)

fun extractImageMetadata(pathInTheBucket: String, default: Image): Image {
    logger.info("Extracting for $pathInTheBucket")
    var tempFile: File? = null
    try {
        tempFile = Files.createTempFile("", ".jpg").toFile()
        tempFile.deleteOnExit()

        val blob = storage.get(BlobId.of(bucketName, pathInTheBucket))
        blob.downloadTo(tempFile.toPath())

        return extractImageData(default, tempFile)
    } catch (t: Throwable) {
        logger.error("Unable to extract data for $pathInTheBucket: ${t.message}", t)
        return default
    } finally {
        if (tempFile != null) {
            try {
                tempFile.delete()
            } catch (t: Throwable) {
                logger.error("Error deleting temp file: ${t.message}", t)
            }
        }
    }
}

fun extractImageData(image: Image, file: File): Image {
    try {
        val metadata = ImageMetadataReader.readMetadata(file)

        var imageMetadata = ImageMetadata(image.timestamp, image.description, image.imageId)
        metadata.directories
            .forEach { directory ->
                when (directory.name) {
                    "Exif SubIFD" -> imageMetadata = getImageExifData(directory.tags)
                    "GPS" -> imageMetadata.gps = getImageGpsData(directory.tags)
                }
                setPresetImageGps(imageMetadata)
            }
        return image.copy(
            description = imageMetadata.userComment,
            timestamp = imageMetadata.originalTime,
            gps = imageMetadata.gps
        )
    } catch (t: Throwable) {
        logger.error(t.message)
        return image
    }
}

private fun getImageExifData(tags: Collection<Tag>): ImageMetadata {
    val metadata = ImageMetadata()
    tags.forEach { tag -> when(tag.tagName) {
        "Date/Time Original" -> {
            try {
                metadata.originalTime = SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(tag.description).time
            } catch (e: ParseException) {
                "Unable to format date ${tag.description}: ${e.message}"
            }
        }
        "User Comment" -> metadata.userComment = tag.description
        "Unique Image ID" -> metadata.imageId = tag.description
    } }
    return  metadata
}

private fun setPresetImageGps(imageMetadata: ImageMetadata) {
    if (imageMetadata.gps == null) {
//        GpsData("E", "34.6675113", "N", "67.8458", "Terrain", "0") // Ловозеро
//        imageMetadata.gps = GpsData("E", "81.2944348", "N", "31.0674983", "Terrain", "0") // Kailash
    }
}
