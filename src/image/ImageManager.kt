package info.vlassiev.serg.image

import com.drew.metadata.Directory
import com.google.cloud.storage.*
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageVariant
import info.vlassiev.serg.model.VariantName.*
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

fun resize(imageFile: File, size: Int): File? {
    logger.info("Resizing file $imageFile to ${size}px")
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
        val variants = listOf(
            ".jpg" to DEFAULT,
            "_thumbnail.jpg" to THUMBNAIL,
            "_2048.jpg" to V2048,
            "_1024.jpg" to V1024,
            "_800.jpg" to V800)
            .map { (suffix, variantName) ->
                val fileName = "${this.name}".replace(".jpg", suffix, true)
                val variantLocation = "https://storage.googleapis.com/${this.bucket}/$fileName"
                when (variantName) {
                    THUMBNAIL -> resizeAndUpload(tempFile, fileName, 80)
                    V2048 -> resizeAndUpload(tempFile, fileName, 2048)
                    V1024 -> resizeAndUpload(tempFile, fileName, 1024)
                    V800 -> resizeAndUpload(tempFile, fileName, 800)
                }
                ImageVariant(variantName, variantLocation)
            }
        val draft = Image(
            imageId = UUID.randomUUID().toString(),
            location = variants.first { it.name == DEFAULT }.location,
            thumbnail = variants.first { it.name == THUMBNAIL }.location,
            description = "",
            timestamp = Instant.now().toEpochMilli(),
            variants = variants,
            gps = null
        )
        logger.info("Extract data for ${this.mediaLink}")
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

private fun resizeAndUpload(originalImage: File, uploadPath: String, size: Int) {
    val resizedFile = resize(originalImage, size)
    val content = FileInputStream(resizedFile).readBytes()
    val blobId = BlobId.of(bucketName, uploadPath)
    val blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpg").build()
    logger.info("Uploading to $uploadPath")
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