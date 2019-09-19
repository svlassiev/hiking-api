package info.vlassiev.serg.repository

import com.mongodb.ConnectionString
import com.mongodb.ServerAddress
import info.vlassiev.serg.image.extractImageMetadata
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.model.getFolders
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.slf4j.LoggerFactory
import java.net.InetAddress

class Repository() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connectionString = ConnectionString("mongodb://mongo-0.mongo,mongo-1.mongo,mongo-2.mongo:27017/?replicaSet=test&readPreference=primaryPreferred&connectTimeoutMS=80000&socketTimeoutMS=20000")
    private val serverAddress = ServerAddress(InetAddress.getLocalHost(), 27017)
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
        val images = folder.images.map { image -> extractImageMetadata("Baikal/Picture${image.imageId}.jpg", image) }
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImageList(imageList)
        repository.insertManyImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}