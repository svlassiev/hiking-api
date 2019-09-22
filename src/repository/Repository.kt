package info.vlassiev.serg.repository

import info.vlassiev.serg.image.extractImageMetadata
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.model.getFolders
import org.litote.kmongo.KMongo
import org.litote.kmongo.`in`
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory

class Repository(connectionString: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client by lazy { KMongo.createClient(connectionString) }
    private val database by lazy { client.getDatabase("colorless-days-children") }
    private val imageListsCollection by lazy { database.getCollection<ImageList>() }
    private val imagesCollection by lazy { database.getCollection<Image>() }

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