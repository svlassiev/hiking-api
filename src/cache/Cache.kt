package info.vlassiev.serg.cache

import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.image.ImageClient.TimelineItem
import info.vlassiev.serg.model.Image
import info.vlassiev.serg.model.VariantName

// Rebuilt at start-up and after every edit (ImageClient.refreshCaches). Each is replaced whole by
// a freshly built immutable value, so a reader sees either the old one or the new one; @Volatile
// makes the swap visible to the request threads straight away.
@Volatile private var timelineHead: List<TimelineItem> = emptyList()
@Volatile private var timelineTail: List<TimelineItem> = emptyList()
@Volatile private var images: Map<String, Image> = emptyMap()

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
    // Both halves are loaded before either is published: if loading the tail throws, readers keep
    // a consistent old timeline instead of a new head on top of an old tail.
    val head = imageClient.loadTimelineData(head = true, tail = false)
    val tail = imageClient.loadTimelineData(head = false, tail = true)
    timelineHead = head
    timelineTail = tail
}

fun resetImageCache(imageClient: ImageClient) {
    val imageIds = getTimelineDataCache().mapNotNull { it.imageId }
    // A plain map keyed by id. This used to be a TreeMap ORDERED BY TIMESTAMP, and a TreeMap treats
    // keys that compare equal as one key: every two photos taken in the same second (a burst, or
    // all photos without an EXIF date) collapsed into a single entry, so `images` answered one of
    // them for both ids. Nothing ever used the order — lookups are by id, in request order.
    images = imageClient.findImages(imageIds, skip = 0, limit = imageIds.size)
        .map { it.copy(location = it.variants.find { v -> v.name == VariantName.V1024 }?.location ?: it.location) }
        .associateBy { it.imageId }
}