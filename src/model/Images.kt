package info.vlassiev.serg.model

import java.time.Instant.now
import java.util.*

data class Image(val imageId: String, val location: String, val thumbnail: String, val description: String, val timestamp: Long, val gps: GpsData? = null)
data class ImageList(val folderId: String = UUID.randomUUID().toString(), val name: String, val images: List<String>)
data class Folder(val listId: String, val name: String, val images: List<Image>) {
    constructor(listId: String, name: String, count: Int) : this(listId, name, getImagesForFolder(listId, count))
}
data class GpsData(var latitudeRef: String = "", var latitude: String = "", var longitudeRef: String = "", var longitude: String = "", var altitudeRef: String = "", var altitude: String = "")

fun getFolders(): List<Folder> {
    return listOf(
        Folder("Baikal", "Байкал 2004", 127),
        Folder("4thmay", "Четвёртый майский", 79),
        Folder("4trados", "Четвёртый осенний", 26),
        Folder("5tradmay", "Пятый майский", 44),
        Folder("6tradmay", "Шестой майский", 55),
        Folder("6trados", "Шестой осенний", 41),
        Folder("7tradmay", "Седьмой майский", 12),
        Folder("7trados", "Седьмой осенний", 41),
        Folder("Hibiny9", "Хибины 2009", 138),
        Folder("ArcticCircleBeyound", "Поход за полярный круг", 99),
        Folder("canoeing11", "Байдарочный поход 2011", 98)
    )
}

fun getImagesForFolder(folder: String, count: Int): List<Image> {
    return List(count) { index ->
        val imageSuffix = "00${index + 1}".takeLast(3)
        Image(
            imageId = imageSuffix,
            location = "https://storage.cloud.google.com/colorless-days-children/$folder/Picture$imageSuffix.jpg",
            thumbnail = "https://storage.cloud.google.com/colorless-days-children/$folder/1_Picture$imageSuffix.jpg",
            description = "$folder ${index + 1}",
            timestamp = now().toEpochMilli()
        )
    }
}