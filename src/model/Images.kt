package info.vlassiev.serg.model

import java.time.Instant.now
import java.util.*

data class Image(val imageId: String, val location: String, val thumbnail: String, val description: String, val timestamp: Long, val gps: GpsData? = null)
data class ImageList(val listId: String = UUID.randomUUID().toString(), val name: String, val images: List<String>)
data class Folder(val listId: String, val name: String, val images: List<Image>, val prefix: String = "Picture", val postfix: String = ".jpg") {
    constructor(listId: String, name: String, count: Int, prefix: String = "Picture", postfix: String = ".jpg") : this(listId, name, getImagesForFolder(listId, count, prefix, postfix), prefix, postfix)
}
data class GpsData(var latitudeRef: String = "", var latitude: String = "", var longitudeRef: String = "", var longitude: String = "", var altitudeRef: String = "", var altitude: String = "")

fun getFolders(): List<Folder> {
    return listOf(
//        Folder("pohod", "Второй майский", 36, "pohod"),
//        Folder("pohod27-2806", "Поход знакомств", 58, "pohod")
    )
}

fun getImagesForFolder(folder: String, count: Int, prefix: String, postfix: String): List<Image> {
    return List(count) { index ->
        val imageSuffix = "00${index + 1}".takeLast(3)
        Image(
            imageId = imageSuffix,
            location = "https://storage.googleapis.com/colorless-days-children/$folder/$prefix$imageSuffix$postfix",
            thumbnail = "https://storage.googleapis.com/colorless-days-children/$folder/1_$prefix$imageSuffix$postfix",
            description = "$folder ${index + 1}",
            timestamp = now().toEpochMilli()
        )
    }
}