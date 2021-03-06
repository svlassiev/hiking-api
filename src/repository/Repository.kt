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

    fun insertImagesList(list: ImageList) {
        logger.info("Inserting ImageList ${list.name}")
        imageListsCollection.insertOne(list)
        logger.info("ImageList ${list.name} is inserted")
    }

    fun deleteImagesLists(listIds: List<String>) {
        logger.info("Deleting ImageLists $listIds")
        imageListsCollection.deleteMany(ImageList::listId `in` listIds)
        logger.info("${listIds.size} ImageLists are deleted")
    }

    fun insertImage(image: Image) {
        logger.info("Inserting image $image")
        imagesCollection.insertOne(image)
        logger.info("Image is inserted")
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

    fun deleteImageFromList(listId: String, imageId: String) {
        logger.info("Deleting image $imageId from list $listId")
        val originalList = imageListsCollection.findOne(ImageList::listId eq listId) ?: return
        val updatedList = originalList.copy(images = originalList.images.filter { it != imageId } )
        imageListsCollection.updateOne(ImageList::listId eq listId, updatedList)
        logger.info("Image is deleted")
    }

    fun findImagesList(imagesListId: String): ImageList {
        logger.info("Finding images list $imagesListId")
        return imageListsCollection.findOne(ImageList::listId eq imagesListId) ?: throw NoSuchElementException("No $imagesListId in Images Lists")
    }

    fun findImagesLists(): List<ImageList> {
        logger.info("Finding all images lists")
        return imageListsCollection.find().filterNotNull()
    }

    fun findImages(imageIds: List<String>, skip: Int, limit: Int): List<Image> {
        logger.info("Finding images for ids: $imageIds")
        val pipeline = listOfNotNull(
            match(Image::imageId `in` imageIds),
            sort(ascending(Image::timestamp)),
            if (limit > 0) limit(skip + limit) else null,
            if (skip > 0) skip(skip) else null
        )
        return imagesCollection.aggregate(pipeline).filterNotNull()
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

    fun replaceImagesList(list: ImageList) {
        logger.info("Updating images list ${list.listId}")
        imageListsCollection.replaceOne(ImageList::listId eq list.listId, list)
    }

    fun updateImagesListName(listId: String, newName: String) {
        logger.info("Updating images list $listId name $newName")
        imageListsCollection.updateMany(
            ImageList::listId eq listId,
            setValue(ImageList::name, newName)
        )
    }

    fun updateImageDescription(imageId: String, description: String) {
        logger.info("Updating image $imageId description $description")
        imagesCollection.updateMany(
            Image::imageId eq imageId,
            setValue(Image::description, description)
        )
    }
}
