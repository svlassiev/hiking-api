package info.vlassiev.serg.image

import com.drew.metadata.Directory
import com.google.cloud.storage.*
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageVariant
import info.vlassiev.serg.model.VariantName
import info.vlassiev.serg.model.VariantName.*
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

const val bucketName = "colorless-days-children"
val storage: Storage = StorageOptions.getDefaultInstance().service

private val logger = LoggerFactory.getLogger("ImageManager")

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
        val suffixes = mapOf(DEFAULT to ".jpg", THUMBNAIL to "_thumbnail.jpg", V2048 to "_2048.jpg", V1024 to "_1024.jpg", V800 to "_800.jpg")
        fun objectName(variantName: VariantName) = "${this.name}".replace(".jpg", suffixes.getValue(variantName), true)

        val started = System.currentTimeMillis()
        makeVariants(tempFile) { variantName, file ->
            upload(file, objectName(variantName))
        }
        logger.info("Variants made and uploaded in ${System.currentTimeMillis() - started} ms")
        // Same list, same order as before: the original first, then the copies.
        val variants = listOf(DEFAULT, THUMBNAIL, V2048, V1024, V800)
            .map { ImageVariant(it, "https://storage.googleapis.com/${this.bucket}/${objectName(it)}") }
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

private fun upload(file: File, uploadPath: String) {
    val content = file.readBytes()
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