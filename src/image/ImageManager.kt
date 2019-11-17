package info.vlassiev.serg.image

import com.drew.metadata.Directory
import com.google.cloud.storage.*
import info.vlassiev.serg.model.Folder
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.model.getFolders
import info.vlassiev.serg.repository.Repository
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

const val bucketName = "colorless-days-children"
val storage: Storage = StorageOptions.getDefaultInstance().service

private val logger = LoggerFactory.getLogger("ImageManager")

fun resize(dir: Path, imageFile: File, size: Int, originalName: String = imageFile.nameWithoutExtension) {
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

fun printMetadata(directory: Directory) {
    directory.tags.forEach { println("${directory.name}\t${it.tagName}\t${it.description}") }
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
                val default = Image(UUID.randomUUID().toString(), source, thumbnail, "", Instant.now().toEpochMilli(), null)
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


fun spinUpFolder(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up images")
    logger.info("Starting spin up")
    getFolders().forEach { folder ->
        val images = folder.images.map { image -> extractImageMetadata("${folder.listId}/${folder.prefix}${image.imageId}${folder.postfix}", image) }
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImagesList(imageList)
        repository.upsertImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}

fun spinUpReplaceUrls(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up URLs")
    logger.info("Starting spin up")
    val imageLists = repository.findImagesLists().filter { setOf( "", "", "", "").contains(it.listId) }
    val images = repository.findImages(imageLists.flatMap { it.images })
    val updatedImages = images.map { it.copy(
        location = it.location.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children"),
        thumbnail = it.thumbnail.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children")
    )}
    repository.upsertImages(updatedImages)
    logger.info("Spin up is finished")
}

fun spinUpDeleteWrongData(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up deleting data")
    logger.info("Starting spin up")
    val imageLists = repository.findImagesLists().filter { setOf( "", "", "", "").contains(it.listId) }
    val imageIds = imageLists.flatMap { it.images }
    repository.deleteImages(imageIds)
    repository.deleteImagesLists(imageLists.map { it.listId })
    logger.info("Spin up if finished")
}

fun spinUpGoogleapisFolder(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up images for googleapis folders")
    logger.info("Starting spin up for googleapis folders")
    val folders = setOf("source" to "name")
    folders.forEach { (path, name) ->
        val folder = getGoogleapisFolder(path, name)
        val images = folder.images
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImagesList(imageList)
        repository.upsertImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}

fun createSignedUrl(imagesListId: String, imageName: String): URL {
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
    return signedUrl
}