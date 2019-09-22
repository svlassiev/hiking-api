package info.vlassiev.serg.repository

import com.mongodb.ConnectionString
import info.vlassiev.serg.image.extractImageMetadata
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.model.getFolders
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory

class Repository() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connectionString = ConnectionString("mongodb://mongo-cdc-0.mongo-cdc,mongo-cdc-1.mongo-cdc,mongo-cdc-2.mongo-cdc:27017/?replicaSet=rs0&readPreference=primaryPreferred&connectTimeoutMS=80000&socketTimeoutMS=20000")
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
}

fun spinUp() {
    val logger = LoggerFactory.getLogger("Spin Up images")
    logger.info("Starting spin up")
    val repository = Repository()
    getFolders().forEach() { folder ->
        val images = folder.images.map { image -> extractImageMetadata("${folder.listId}/Picture${image.imageId}.jpg", image) }
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImageList(imageList)
        repository.insertManyImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}