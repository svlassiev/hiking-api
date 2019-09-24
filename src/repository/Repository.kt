package info.vlassiev.serg.repository

import com.oracle.util.Checksums.update
import info.vlassiev.serg.image.extractImageMetadata
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.model.getFolders
import org.litote.kmongo.*
import org.slf4j.LoggerFactory

class Repository(connectionString: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = KMongo.createClient(connectionString)
    private val database = client.getDatabase("colorless-days-children")
    private val imageListsCollection = database.getCollection<ImageList>()
    private val imagesCollection = database.getCollection<Image>()

    fun insertImageList(list: ImageList) {
        logger.info("Inserting ImageList ${list.name}")
        imageListsCollection.insertOne(list)
        logger.info("ImageList ${list.name} is inserted")
    }

    fun insertManyImages(images: List<Image>) {
        logger.info("Inserting ${images.size} images")
        imagesCollection.insertMany(images)
        logger.info("Images are inserted")
    }

    fun findImageLists(): List<ImageList> {
        logger.info("Finding all images lists")
        return imageListsCollection.find().filterNotNull()
    }

    fun findImages(imageIds: List<String>): List<Image> {
        logger.info("Finding images for ids: $imageIds")
        return imagesCollection.find(Image::imageId `in` imageIds).filterNotNull()
    }

    fun upsertImages(images: List<Image>) {
        logger.info("Upsert images with ids: ${images.map { it.imageId }}")
        imagesCollection.bulkWrite(
        images.map {image ->
            replaceOne(
                Image::imageId eq image.imageId,
                image,
                upsert()
            )
        })
    }
}

fun spinUp(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up images")
    logger.info("Starting spin up")
    getFolders().forEach() { folder ->
        val images = folder.images.map { image -> extractImageMetadata("${folder.listId}/Picture${image.imageId}.jpg", image) }
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImageList(imageList)
        repository.insertManyImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}

fun spinUpReplaceUrls(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up URLs")
    logger.info("Starting spin up")
    val images = repository.findImages(repository.findImageLists().flatMap { it.images })
    val updatedImages = images.map { it.copy(
        location = it.location.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children"),
        thumbnail = it.thumbnail.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children")
    )}
    repository.upsertImages(updatedImages)
    logger.info("Spin up if finished")
}