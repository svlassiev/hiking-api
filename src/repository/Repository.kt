package info.vlassiev.serg.repository

import info.vlassiev.serg.image.extractImageMetadata
import info.vlassiev.serg.image.getGoogleapisFolder
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

    fun deleteImageLists(listIds: List<String>) {
        logger.info("Deleting ImageLists $listIds")
        imageListsCollection.deleteMany(ImageList::listId `in` listIds)
        logger.info("${listIds.size} ImageLists are deleted")
    }

    fun insertImages(images: List<Image>) {
        logger.info("Inserting ${images.size} images")
        imagesCollection.insertMany(images)
        logger.info("Images are inserted")
    }

    fun deleteImages(images: List<String>) {
        logger.info("Deleting ${images.size} images")
        imagesCollection.deleteMany(Image::imageId `in` images)
        logger.info("Images are deleted")
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
        val images = folder.images.map { image -> extractImageMetadata("${folder.listId}/${folder.prefix}${image.imageId}${folder.postfix}", image) }
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImageList(imageList)
        repository.upsertImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}

fun spinUpReplaceUrls(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up URLs")
    logger.info("Starting spin up")
    val imageLists = repository.findImageLists().filter { it.listId == "" }
    val images = repository.findImages(imageLists.flatMap { it.images })
    val updatedImages = images.map { it.copy(
        location = it.location.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children"),
        thumbnail = it.thumbnail.replace("https://storage.cloud.google.com/colorless-days-children","https://storage.googleapis.com/colorless-days-children")
    )}
    repository.upsertImages(updatedImages)
    logger.info("Spin up if finished")
}

fun spinUpDeleteWrongData(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up deleting data")
    logger.info("Starting spin up")
    val imageLists = repository.findImageLists().filter { it.name in setOf("") }
    val imageIds = imageLists.flatMap { it.images }
    repository.deleteImages(imageIds)
    repository.deleteImageLists(imageLists.map { it.listId })
    logger.info("Spin up if finished")
}

fun spinUpGoogleapisFolder(repository: Repository) {
    val logger = LoggerFactory.getLogger("Spin Up images for googleapis folders")
    logger.info("Starting spin up for googleapis folders")
    val folders = setOf("" to "")
    folders.forEach() { (path, name) ->
        val folder = getGoogleapisFolder(path, name)
        val images = folder.images
        val imageList = ImageList(name = folder.name, images = images.map { it.imageId })
        logger.info("Starting spin up for folder ${folder.name}")
        repository.insertImageList(imageList)
        repository.upsertImages(images)
        logger.info("Spin up for folder ${folder.name} is completed")
    }
}
