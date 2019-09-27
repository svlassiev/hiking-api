package info.vlassiev.serg.image

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Tag
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import info.vlassiev.serg.model.Folder
import info.vlassiev.serg.model.GpsData
import info.vlassiev.serg.model.Image
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant.now
import java.util.*
import javax.imageio.ImageIO

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
                setPresetImageGps(imageMetadata)
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

private fun setPresetImageGps(imageMetadata: ImageMetadata) {
    if (imageMetadata.gps == null) {
//        GpsData("E", "34.6675113", "N", "67.8458", "Terrain", "0") // Ловозеро
//        imageMetadata.gps = GpsData("E", "81.2944348", "N", "31.0674983", "Terrain", "0") // Kailash
    }
}

private fun printMetadata(directory: Directory) {
    directory.tags.forEach { println("${directory.name}\t${it.tagName}\t${it.description}") }
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
        val blobs = storage.list(bucketName, Storage.BlobListOption.prefix(pathInTheBucket))
        val images = blobs.iterateAll().filter { it.name.endsWith(suffix = "jpg", ignoreCase = true) }.mapNotNull { blob ->
            var tempFile: File? = null

            try {
                tempFile = Files.createTempFile("", ".jpg").toFile()
                tempFile.deleteOnExit()
                blob.downloadTo(tempFile.toPath())
                val source = "https://storage.googleapis.com/${blob.bucket}/${blob.name}".replace(".jpg", ".jpg", true)
                val thumbnail = "https://storage.googleapis.com/${blob.bucket}/${blob.name}".replace(".jpg", "_thumbnail.jpg", true)
                val default = Image(UUID.randomUUID().toString(), source, thumbnail, "", now().toEpochMilli(), null)
                logger.info("Extract data for ${blob.mediaLink}")
                val originalName = blob.name.dropLast(4).substringAfterLast("/")
                val localFolder = File("""""").toPath().resolve(pathInTheBucket)
                resize(localFolder, tempFile, 1024, originalName)
                resize(localFolder, tempFile, 800, originalName)
                resize(localFolder, tempFile, 80, originalName)
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

private fun resize(dir: Path, imageFile: File, size: Int, originalName: String = imageFile.nameWithoutExtension) {
    logger.info("Resizing file $imageFile")
    Files.createDirectories(dir)
    var outputFile: File? = null
    try {
        outputFile = Files.createFile(dir.resolve("${originalName}_${if (size < 100) "thumbnail" else "$size"}.jpg")).toFile()
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