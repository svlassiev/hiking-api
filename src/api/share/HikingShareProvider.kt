package info.vlassiev.serg.api.share

import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.model.VariantName

object HikingShareProvider {

    fun resolveAlbum(listId: String, imageClient: ImageClient): Triple<String, String, String>? {
        val lists = imageClient.getAllNonEmptyImagesLists()
        val list = lists.find { it.listId == listId } ?: return null
        val firstImageId = list.images.firstOrNull() ?: return null
        val images = imageClient.findImages(listOf(firstImageId), 0, 1)
        val image = images.firstOrNull() ?: return null
        val imageUrl = image.variants.find { it.name == VariantName.V1024 }?.location ?: image.location
        return Triple(list.name, imageUrl, "${list.images.size} photos")
    }

    fun resolveImage(imageId: String, imageClient: ImageClient): Triple<String, String, String>? {
        val images = imageClient.findImages(listOf(imageId), 0, 1)
        val image = images.firstOrNull() ?: return null
        val imageUrl = image.variants.find { it.name == VariantName.V1024 }?.location ?: image.location
        val lists = imageClient.getAllNonEmptyImagesLists()
        val albumName = lists.find { imageId in it.images }?.name ?: "Hiking"
        return Triple(albumName, imageUrl, image.description.ifBlank { "Hiking photo gallery" })
    }
}
