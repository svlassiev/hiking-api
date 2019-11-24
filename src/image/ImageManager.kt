package info.vlassiev.serg.image

import com.drew.metadata.Directory
import com.google.cloud.storage.*
import info.vlassiev.serg.model.Image
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

const val bucketName = "colorless-days-children"
val storage: Storage = StorageOptions.getDefaultInstance().service

private val logger = LoggerFactory.getLogger("ImageManager")

fun resize(imageFile: File, size: Int, originalName: String = imageFile.nameWithoutExtension): File? {
    logger.info("Resizing file $imageFile")
    var outputFile: File? = null
    try {
        outputFile = Files.createTempFile("", ".jpg").toFile()
        outputFile.deleteOnExit()
        val image = ImageIO.read(imageFile)
        val scaledImage = Scalr.resize(image, if (size < 100) Scalr.Method.SPEED else Scalr.Method.ULTRA_QUALITY, size)
        ImageIO.write(scaledImage, "JPEG", outputFile)
        logger.info("Scaled image is saved to $outputFile")
        return outputFile
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
    return outputFile
}

fun printMetadata(directory: Directory) {
    directory.tags.forEach { println("${directory.name}\t${it.tagName}\t${it.description}") }
}

fun generateGoogleapisImage(pathInTheBucket: String): Image {
    logger.info("Getting data for $pathInTheBucket")
    val blob = storage.get(BlobId.of(bucketName, pathInTheBucket))
    return blob.toImage()
}

private fun Blob.toImage(): Image {
    var tempFile: File? = null

    return try {
        tempFile = Files.createTempFile("", ".jpg").toFile()
        tempFile.deleteOnExit()
        this.downloadTo(tempFile.toPath())
        val source = "https://storage.googleapis.com/${this.bucket}/${this.name}"
        val thumbnailName = "${this.name}".replace(".jpg", "_thumbnail.jpg", true)
        val thumbnail = "https://storage.googleapis.com/${this.bucket}/$thumbnailName"
        val draft = Image(UUID.randomUUID().toString(), source, thumbnail, "", Instant.now().toEpochMilli(), null)
        logger.info("Extract data for ${this.mediaLink}")
        uploadThumbnail(tempFile, thumbnailName)
        extractImageData(draft, tempFile)
    } catch (t: Throwable) {
        logger.error("Unable to extract data for file $tempFile: ${t.message}", t)
        throw t
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

private fun uploadThumbnail(originalImage: File, uploadPath: String) {
    val thumbnailFile = resize(originalImage, 80)
    val content = FileInputStream(thumbnailFile).readBytes()
    val blobId = BlobId.of(bucketName, uploadPath)
    val blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpg").build()
    storage.create(blobInfo, content)
}

fun createSignedUrl(imagesListId: String, imageName: String): SignedUrlResponse {
    val objectName = "$imagesListId/$imageName"

    val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()

    val extensionHeaders = mapOf("Content-Type" to "image/jpeg")
    val signedUrl = storage.signUrl(
        blobInfo,
        15,
        TimeUnit.MINUTES,
        Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
        Storage.SignUrlOption.withExtHeaders(extensionHeaders),
        Storage.SignUrlOption.withV4Signature())

    logger.info("Signed URL is generated: $signedUrl")
    return SignedUrlResponse(signedUrl, objectName)
}

data class SignedUrlResponse(val signedUrl: URL, val location: String)