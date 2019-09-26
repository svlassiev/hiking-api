package info.vlassiev.serg.image

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Tag
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.StorageOptions
import info.vlassiev.serg.model.GpsData
import info.vlassiev.serg.model.Image
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


private val logger = LoggerFactory.getLogger("MetadataExtractor")

private fun extract(image: Image, file: File): Image {
    try {
        val metadata = ImageMetadataReader.readMetadata(file)

        var imageMetadata = ImageMetadata(image.timestamp, image.description, image.imageId)
        metadata.directories
            .forEach { directory ->
                when (directory.name) {
                    "Exif SubIFD" -> imageMetadata = getImageExifData(directory.tags)
                    "GPS" -> imageMetadata.gps = getImageGpsData(directory.tags)
                }
            }
        return Image(
            imageId = UUID.randomUUID().toString(),
            location = image.location,
            thumbnail = image.thumbnail,
            description = imageMetadata.userComment,
            timestamp = imageMetadata.originalTime,
            gps = imageMetadata.gps
        )
    } catch (t: Throwable) {
        logger.error(t.message)
        return image.copy(imageId = UUID.randomUUID().toString())
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

private fun getImageGpsData(tags: Collection<Tag>): GpsData {
    val gps = GpsData()
    tags.forEach { tag -> when(tag.tagName) {
        "Latitude Ref" -> gps.latitudeRef = tag.description
        "Latitude" -> gps.latitude = tag.description
        "Longitude Ref" -> gps.longitudeRef = tag.description
        "Longitude" -> gps.longitude = tag.description
        "Altitude Ref" -> gps.altitudeRef = tag.description
        "Altitude" -> gps.altitude = tag.description
    } }
    return gps
}

private data class ImageMetadata(var originalTime: Long = 0, var userComment: String = "", var imageId: String = UUID.randomUUID().toString(), var gps: GpsData? = null)

private const val bucketName = "colorless-days-children"
private val storage = StorageOptions.getDefaultInstance().service

fun extractImageMetadata(pathInTheBucket: String, default: Image): Image {
    logger.info("Extracting for $pathInTheBucket")
    var tempFile: File? = null
    try {
        tempFile = Files.createTempFile("", ".jpg").toFile()
        tempFile.deleteOnExit()

        val blob = storage.get(BlobId.of(bucketName, pathInTheBucket))
        blob.downloadTo(tempFile.toPath())

        return extract(default, tempFile)
    } catch (t: Throwable) {
        logger.error("Unable to extract data for $pathInTheBucket: ${t.message}", t)
        return default.copy(imageId = UUID.randomUUID().toString())
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

fun getGoogleapisFolder(pathInTheBucket: String, name: String): Folder {
    logger.info("Getting data for $pathInTheBucket")
    try {
        val blobs = storage.list(
            bucketName, BlobListOption.prefix(pathInTheBucket)
        )
        val images = blobs.iterateAll().mapNotNull { blob ->
            var tempFile: File? = null

            try {
                tempFile = Files.createTempFile("", ".jpg").toFile()
                tempFile.deleteOnExit()
                blob.downloadTo(tempFile.toPath())
                val default = Image(UUID.randomUUID().toString(), blob.mediaLink, blob.mediaLink, "", now().toEpochMilli(), null)
                logger.info("Extract data for ${blob.mediaLink}")
                extract(default, tempFile)
            } catch (t: Throwable) {
                logger.error("Unable to extract data for $pathInTheBucket: ${t.message}", t)
                null
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
        return Folder(UUID.randomUUID().toString(), name, images)
    } catch (t: Throwable) {
        logger.error("Unable to extract data for $pathInTheBucket: ${t.message}", t)
        throw t
    }
}

private fun resize(dir: Path, imageFile: File, size: Int) {
    logger.info("Resizing file $imageFile")
    var outputFile: File? = null
    try {
        outputFile = Files.createFile(dir.resolve("${imageFile.nameWithoutExtension}_${if (size < 100) "thumbnail" else "$size"}.jpg")).toFile()
        val image = ImageIO.read(imageFile)
        val scaledImage = Scalr.resize(image, if (size < 100) Scalr.Method.SPEED else Scalr.Method.ULTRA_QUALITY, size)
        ImageIO.write(scaledImage, "JPEG", outputFile)
        logger.info("Scaled image is saved to $outputFile")
    } catch (t: Throwable) {
        logger.error("Unable to resize image $imageFile", t)
        if (outputFile != null) {
            try {
                outputFile.delete()
            } catch (t: Throwable) {
                logger.error("Error deleting failed file: ${t.message}", t)
            }
        }
    }
}

fun spinUpResize() {
    logger.info("Spin up images resize")
    val fileNames = setOf("""""")
    val outputDirectory = Files.createDirectories(File("""""").toPath())
    fileNames.forEach { name ->
        resize(outputDirectory, File(name), 80)
        resize(outputDirectory, File(name), 800)
        resize(outputDirectory, File(name), 1024)
    }
    logger.info("Spin up is over")
}
