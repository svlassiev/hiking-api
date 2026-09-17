package info.vlassiev.serg.image

import info.vlassiev.serg.cache.getTimelineDataCache
import info.vlassiev.serg.cache.getimagesCache
import info.vlassiev.serg.cache.initializeCaches
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.repository.Repository
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class ImageClient(private val repository: Repository) {

    private val logger = LoggerFactory.getLogger(ImageClient::class.java)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    /**
     * The timeline and the `images` endpoint are served from caches that used to be filled only
     * at start-up, so nothing edited here was visible to readers until the next deploy. Every
     * successful edit now rebuilds them from the database.
     *
     * A failed rebuild must NOT fail the edit: the edit is already in the database, and a client
     * told that a registration failed will register the photo again. The stale cache is logged
     * and left for the next edit (or restart) to repair.
     */
    private fun refreshCaches() {
        try {
            val started = System.currentTimeMillis()
            initializeCaches(this)
            logger.info("Caches rebuilt in ${System.currentTimeMillis() - started} ms")
        } catch (e: Exception) {
            logger.error("Edit saved, but rebuilding the caches failed; readers see stale data until the next edit", e)
        }
    }

    fun getTimelineData(head: Boolean = true, tail: Boolean = true): List<TimelineItem> =
        getTimelineDataCache(head, tail).let { if (it.isEmpty()) loadTimelineData(head, tail) else it }

    fun loadTimelineData(head: Boolean = true, tail: Boolean = true): List<TimelineItem> {
        return getAllImagesLists()
            .filterIndexed{ index, _ -> (head && index == 0) || (tail && index > 0) }
            .flatMap { list ->
                val title = TimelineItem(imageId = null, title = list.name, date = null, listId = list.listId)
                val imageData = findImages(imageIds = list.images, skip = 0, limit = list.images.size).
                    groupBy { image -> dateFormat.format(image.timestamp) }.
                    flatMap { (date, images) ->
                        listOf(TimelineItem(imageId = null, title = null, date = date, listId = list.listId)) +
                                images.
                                    sortedBy { it.timestamp }.
                                    map { TimelineItem(imageId = it.imageId, title = null, date = null, listId = list.listId) }
                    }

                listOf(title) + imageData
            }
    }
    data class TimelineItem(val imageId: String?, val title: String?, val date: String?, val listId: String)

    fun getAllNonEmptyImagesLists(): List<ImageList> {
        val firstImageIdToList = repository.findImagesLists().filter { it.images.isNotEmpty() }.map { list -> list.images[0] to list }.toMap()
        val firstImageFromList = findImages(imageIds = firstImageIdToList.keys.toList(), skip = 0, limit = firstImageIdToList.size)
        return firstImageFromList.sortedByDescending { image -> image.timestamp }.mapNotNull { firstImage -> firstImageIdToList[firstImage.imageId] }
    }

    fun getAllImagesLists(): List<ImageList> {
        val imagesLists= repository.findImagesLists()
        val firstImageIdToList = imagesLists.filter { it.images.isNotEmpty() }.map { list -> list.images[0] to list }.toMap()
        val firstImageFromList = findImages(imageIds = firstImageIdToList.keys.toList(), skip = 0, limit = firstImageIdToList.size)
        val sortedListsWithImages = firstImageFromList.sortedByDescending { image -> image.timestamp }.mapNotNull { firstImage -> firstImageIdToList[firstImage.imageId] }
        return imagesLists.filter { it.images.isEmpty() } + sortedListsWithImages
    }

    fun getImages(imageIds: List<String>, skip: Int, limit: Int): List<Image> = getimagesCache(imageIds, skip, limit)
    fun findImages(imageIds: List<String>, skip: Int, limit: Int): List<Image> {
        return repository.findImages(imageIds, skip, limit)
    }

    fun getEditPageData(): EditPageData {
        return EditPageData(getAllImagesLists())
    }

    data class EditPageData(val imagesLists: List<ImageList>)

    fun updateImagesListName(listId: String, request: UpdateListNameRequest) {
        repository.updateImagesListName(listId, request.listName)
        refreshCaches()
    }

    data class UpdateListNameRequest(val listName: String)

    fun updateImageDescription(imageId: String, request: UpdateImageDescriptionRequest) {
        repository.updateImageDescription(imageId, request.description)
        refreshCaches()
    }

    data class UpdateImageDescriptionRequest(val description: String)

    fun addImagesList(imagesList: ImageList) {
        repository.insertImagesList(imagesList)
        refreshCaches()
    }

    fun deleteImagesList(listId: String) {
        repository.deleteImagesLists(listOf(listId))
        refreshCaches()
    }

    fun deleteImage(listId: String, imageId: String) {
        repository.deleteImageFromList(listId, imageId)
        repository.deleteImages(listOf(imageId))
        refreshCaches()
    }

    fun addImageFromGoogleStorage(request: AddImageRequest): Image {
        val image = generateGoogleapisImage(request.location)
        repository.insertImage(image)
        val list = repository.findImagesList(request.listId)
        val updatedList = list.copy(images = list.images + image.imageId)
        repository.replaceImagesList(updatedList)
        refreshCaches()
        return image
    }

    data class AddImageRequest(val listId: String, val location: String)

}