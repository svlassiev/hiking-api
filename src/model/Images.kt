package info.vlassiev.serg.model

import java.time.Instant.now

data class Image(val imageId: String, val description: String, val timestamp: Long)
data class ImageList(val listId: String, val name: String, val images: List<Image>) {
    constructor(listId: String, name: String, count: Int) : this(listId, name, getImagesForFolder(listId, count))
}

fun getFolders(): List<ImageList> {
    return listOf(
        ImageList("Baikal", "Байкал 2004", 127),
        ImageList("4thmay", "Четвёртый майский", 79),
        ImageList("4trados", "Четвёртый осенний", 26),
        ImageList("5tradmay", "Пятый майский", 44),
        ImageList("6tradmay", "Шестой майский", 55),
        ImageList("6trados", "Шестой осенний", 41),
        ImageList("7tradmay", "Седьмой майский", 12),
        ImageList("7trados", "Седьмой осенний", 41),
        ImageList("Hibiny9", "Хибины 2009", 138),
        ImageList("ArcticCircleBeyound", "Поход за полярный круг", 99),
        ImageList("canoeing11", "Байдарочный поход 2011", 98)
    )
}

private fun getImagesForFolder(folder: String, count: Int): List<Image> {
    return List(count) { index ->
        Image("00${index + 1}".takeLast(3), "$folder ${index + 1}", now().toEpochMilli())
    }
}