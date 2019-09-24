package info.vlassiev.serg.model

import java.time.Instant.now
import java.util.*

data class Image(val imageId: String, val location: String, val thumbnail: String, val description: String, val timestamp: Long, val gps: GpsData? = null)
data class ImageList(val listId: String = UUID.randomUUID().toString(), val name: String, val images: List<String>)
data class Folder(val listId: String, val name: String, val images: List<Image>) {
    constructor(listId: String, name: String, count: Int) : this(listId, name, getImagesForFolder(listId, count))
}
data class GpsData(var latitudeRef: String = "", var latitude: String = "", var longitudeRef: String = "", var longitude: String = "", var altitudeRef: String = "", var altitude: String = "")

fun getFolders(): List<Folder> {
    return listOf(
    )
}

fun getImagesForFolder(folder: String, count: Int): List<Image> {
    return List(count) { index ->
        val imageSuffix = "00${index + 1}".takeLast(3)
        Image(
            imageId = imageSuffix,
            location = "https://storage.googleapis.com/colorless-days-children/$folder/Picture$imageSuffix.jpg",
            thumbnail = "https://storage.googleapis.com/colorless-days-children/$folder/1_Picture$imageSuffix.jpg",
            description = "$folder ${index + 1}",
            timestamp = now().toEpochMilli()
        )
    }
}