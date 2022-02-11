package info.vlassiev.serg.cache

import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.image.ImageClient.TimelineItem
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.VariantName
import java.util.*

private var timelineHead: List<TimelineItem> = emptyList()
private var timelineTail: List<TimelineItem> = emptyList()
private var images: SortedMap<String, Image> = emptyMap<String, Image>().toSortedMap()

fun getTimelineDataCache(head: Boolean = true, tail: Boolean = true): List<TimelineItem> {
    return if (head && tail) {
        timelineHead + timelineTail
    } else if (head) {
        timelineHead
    } else if (tail) {
        timelineTail
    } else {
        emptyList()
    }
}

fun getimagesCache(imageIds: List<String>, skip: Int, limit: Int): List<Image> {
    val fullList = imageIds.mapNotNull { images[it] }
    if (skip >= fullList.size) {
        return emptyList()
    }
    val toIndex = if (skip + limit > fullList.size) fullList.size else skip + limit
    return fullList.subList(fromIndex = skip, toIndex = toIndex)
}

fun initializeCaches(imageClient: ImageClient) {
    resetTimelineDataCache(imageClient)
    resetImageCache(imageClient)
}

fun resetTimelineDataCache(imageClient: ImageClient) {
    timelineHead = imageClient.loadTimelineData(head = true, tail = false)
    timelineHead = imageClient.loadTimelineData(head = false, tail = true)
}

fun resetImageCache(imageClient: ImageClient) {
    val imageIds = getTimelineDataCache().mapNotNull { it.imageId }
    val idToImage = imageClient.findImages(imageIds, skip = 0, limit = imageIds.size)
        .map { it.copy(location = it.variants.find { v -> v.name == VariantName.V1024 }?.location ?: it.location) }
        .sortedByDescending { it.timestamp }
        .map { it.imageId to it }
        .toMap()
    images = idToImage.toSortedMap(compareBy { idToImage[it]?.timestamp })
}