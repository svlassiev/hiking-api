package info.vlassiev.serg.image

import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.repository.Repository

class ImageClient(private val repository: Repository) {

    fun getAllNonEmptyImagesLists(): List<ImageList> {
        val firstImageIdToList = repository.findImagesLists().filter { it.images.isNotEmpty() }.map { list -> list.images[0] to list }.toMap()
        val firstImageFromList = findImages(firstImageIdToList.keys.toList())
        return firstImageFromList.sortedByDescending { image -> image.timestamp }.mapNotNull { firstImage -> firstImageIdToList[firstImage.imageId] }
    }

    fun getAllImagesLists(): List<ImageList> {
        val imagesLists= repository.findImagesLists()
        val firstImageIdToList = imagesLists.filter { it.images.isNotEmpty() }.map { list -> list.images[0] to list }.toMap()
        val firstImageFromList = findImages(firstImageIdToList.keys.toList())
        val sortedListsWithImages = firstImageFromList.sortedByDescending { image -> image.timestamp }.mapNotNull { firstImage -> firstImageIdToList[firstImage.imageId] }
        return imagesLists.filter { it.images.isEmpty() } + sortedListsWithImages
    }

    fun findImages(imageIds: List<String>): List<Image> {
        return repository.findImages(imageIds).sortedBy { it.timestamp }
    }

    fun getEditPageData(): EditPageData {
        return EditPageData(getAllImagesLists())
    }

    data class EditPageData(val imagesLists: List<ImageList>)

    fun updateImagesListName(listId: String, request: UpdateListNameRequest) {
        repository.updateImagesListName(listId, request.listName)
    }

    data class UpdateListNameRequest(val listName: String)

    fun updateImageDescription(imageId: String, request: UpdateImageDescriptionRequest) {
        repository.updateImageDescription(imageId, request.description)
    }

    data class UpdateImageDescriptionRequest(val description: String)

    fun addImagesList(imagesList: ImageList) {
        repository.insertImagesList(imagesList)
    }

    fun deleteImagesList(listId: String) {
        repository.deleteImagesLists(listOf(listId))
    }

    fun addImageFromGoogleStorage(request: AddImageRequest) {
    }

    data class AddImageRequest(val listId: String, val location: String)

}