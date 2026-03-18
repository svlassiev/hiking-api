package info.vlassiev.serg.api.share

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ColorlessShareProvider")

private const val GCS_BASE = "https://storage.googleapis.com/colorless-days-children"
private const val ALBUMS_URL = "http://colorless-days-children/albums.json"
private const val ALBUMS_FILES_URL = "http://colorless-days-children/albums-files.json"

data class ColorlessAlbum(
    val title: String,
    val folder: String,
    val count: Int,
    val pathName: String,
    @SerializedName("useFiles") val useFiles: Boolean = false
)

data class ColorlessAlbumFiles(
    val folder: String,
    val files: List<String>
)

object ColorlessShareProvider {

    private var albums: List<ColorlessAlbum>? = null
    private var albumFiles: Map<String, List<String>>? = null

    private fun loadAlbums(): List<ColorlessAlbum> {
        albums?.let { return it }
        return try {
            val json = java.net.URL(ALBUMS_URL).readText()
            Gson().fromJson(json, Array<ColorlessAlbum>::class.java).toList().also { albums = it }
        } catch (e: Exception) {
            logger.error("Failed to load albums.json: ${e.message}")
            emptyList()
        }
    }

    private fun loadAlbumFiles(): Map<String, List<String>> {
        albumFiles?.let { return it }
        return try {
            val json = java.net.URL(ALBUMS_FILES_URL).readText()
            Gson().fromJson(json, Array<ColorlessAlbumFiles>::class.java)
                .associate { it.folder to it.files }
                .also { albumFiles = it }
        } catch (e: Exception) {
            logger.error("Failed to load albums-files.json: ${e.message}")
            emptyMap()
        }
    }

    fun resolveAlbum(folder: String): Pair<String, String>? {
        val album = loadAlbums().find { it.folder == folder } ?: return null
        val imageUrl = resolvePhotoUrl(album, 1) ?: return null
        return album.title to imageUrl
    }

    fun resolvePhoto(folder: String, n: Int): Pair<String, String>? {
        val album = loadAlbums().find { it.folder == folder } ?: return null
        val imageUrl = resolvePhotoUrl(album, n) ?: return null
        return album.title to imageUrl
    }

    private fun resolvePhotoUrl(album: ColorlessAlbum, n: Int): String? {
        if (album.useFiles) {
            val files = loadAlbumFiles()[album.folder] ?: return null
            val idx = n - 1
            if (idx < 0 || idx >= files.size) return null
            val filename = files[idx]
            val base = filename.replace(Regex("\\.jpg$", RegexOption.IGNORE_CASE), "")
            return "$GCS_BASE/${album.folder}/${base}_1024.jpg"
        }
        if (n < 1 || n > album.count) return null
        val nnn = n.toString().padStart(3, '0')
        return "$GCS_BASE/${album.folder}/${album.pathName}$nnn.jpg"
    }
}
