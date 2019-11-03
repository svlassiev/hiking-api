package info.vlassiev.serg.image

import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.ImageList
import info.vlassiev.serg.repository.Repository

class ImageClient(private val repository: Repository) {

    fun getAllImageLists(): List<ImageList> {
        val firstImageIdToList = repository.findImageLists().filter { it.images.isNotEmpty() }.map { list -> list.images[0] to list }.toMap()
        val firstImageFromList = findImages(firstImageIdToList.keys.toList())
        return firstImageFromList.sortedByDescending { image -> image.timestamp }.mapNotNull { firstImage -> firstImageIdToList[firstImage.imageId] }
    }

    fun findImages(imageIds: List<String>): List<Image> {
        return repository.findImages(imageIds).sortedBy { it.timestamp }
    }

    fun getEditPageData(): EditPageData {
        return EditPageData(getAllImageLists())
    }

    data class EditPageData(val imageLists: List<ImageList>)

}