package info.vlassiev.serg.repository

import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
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
                filter = Image::imageId eq image.imageId,
                replacement = image,
                options = upsert()
            )
        })
    }

    fun updateImagesListName(listId: String, newName: String) {
        logger.info("Updating images list $listId name $newName")
        imageListsCollection.updateMany(
            ImageList::listId eq listId,
            setValue(ImageList::name, newName)
        )
    }
}
