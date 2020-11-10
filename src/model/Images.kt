package info.vlassiev.serg.model

import java.time.Instant.now
import java.util.*

data class Image(
    val imageId: String,
    val location: String,
    val thumbnail: String,
    val description: String,
    val timestamp: Long,
    val variants: List<ImageVariant>,
    val gps: GpsData? = null
)
data class ImageVariant(val name: VariantName, val location: String)
enum class VariantName { DEFAULT, THUMBNAIL, V2048, V1024, V800 }
data class ImageList(val listId: String = UUID.randomUUID().toString(), val name: String, val images: List<String>)
data class Folder(val listId: String, val name: String, val images: List<Image>, val prefix: String = "Picture", val postfix: String = ".jpg") {
    constructor(listId: String, name: String, count: Int, prefix: String = "Picture", postfix: String = ".jpg") : this(listId, name, getImagesForFolder(listId, count, prefix, postfix), prefix, postfix)
}
data class GpsData(var latitudeRef: String = "", var latitude: String = "", var longitudeRef: String = "", var longitude: String = "", var altitudeRef: String = "", var altitude: String = "")

fun getFolders(): List<Folder> {
    return listOf(
        Folder("Progulka", "Поход прогулка", 53, postfix = ".JPG")
    )
}

fun getImagesForFolder(folder: String, count: Int, prefix: String, postfix: String): List<Image> {
    return List(count) { index ->
        val imageSuffix = "00${index + 1}".takeLast(3)
        val location = "https://storage.googleapis.com/colorless-days-children/$folder/$prefix$imageSuffix$postfix"
        val thumbnail = "https://storage.googleapis.com/colorless-days-children/$folder/1_$prefix$imageSuffix$postfix"
        Image(
            imageId = imageSuffix,
            location = location,
            thumbnail = thumbnail,
            description = "$folder ${index + 1}",
            timestamp = now().toEpochMilli(),
            variants = listOf(ImageVariant(VariantName.DEFAULT, location), ImageVariant(VariantName.THUMBNAIL, thumbnail))
        )
    }
}