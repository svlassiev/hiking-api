package info.vlassiev.serg.api.share

import info.vlassiev.serg.image.ImageClient
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.*

fun Routing.shareApi(path: String, imageClient: ImageClient): Route {
    return route(path) {

        // GET /share/hiking/album/{listId}
        get("/hiking/album/{listId}") {
            val listId = call.parameters["listId"] ?: return@get call.respondText("Missing listId", status = HttpStatusCode.BadRequest)
            val result = HikingShareProvider.resolveAlbum(listId, imageClient)
                ?: return@get call.respondText("Album not found", status = HttpStatusCode.NotFound)
            val (title, imageUrl, description) = result
            call.respondText(
                renderShareHtml(title, imageUrl, description, "/hiking"),
                shareContentType
            )
        }

        // GET /share/hiking/image/{imageId}
        get("/hiking/image/{imageId}") {
            val imageId = call.parameters["imageId"] ?: return@get call.respondText("Missing imageId", status = HttpStatusCode.BadRequest)
            val result = HikingShareProvider.resolveImage(imageId, imageClient)
                ?: return@get call.respondText("Image not found", status = HttpStatusCode.NotFound)
            val (title, imageUrl, description) = result
            call.respondText(
                renderShareHtml(title, imageUrl, description, "/hiking"),
                shareContentType
            )
        }

        // GET /share/{folder}/{n} — colorless photo
        get("/{folder}/{n}") {
            val folder = call.parameters["folder"] ?: return@get call.respondText("Missing folder", status = HttpStatusCode.BadRequest)
            val n = call.parameters["n"]?.toIntOrNull() ?: return@get call.respondText("Invalid photo number", status = HttpStatusCode.BadRequest)
            val result = ColorlessShareProvider.resolvePhoto(folder, n)
                ?: return@get call.respondText("Photo not found", status = HttpStatusCode.NotFound)
            val (title, imageUrl) = result
            call.respondText(
                renderShareHtml("$title — $n", imageUrl, "serg.vlassiev.info", "/"),
                shareContentType
            )
        }

        // GET /share/{folder} — colorless album
        get("/{folder}") {
            val folder = call.parameters["folder"] ?: return@get call.respondText("Missing folder", status = HttpStatusCode.BadRequest)
            val result = ColorlessShareProvider.resolveAlbum(folder)
                ?: return@get call.respondText("Album not found", status = HttpStatusCode.NotFound)
            val (title, imageUrl) = result
            call.respondText(
                renderShareHtml(title, imageUrl, "serg.vlassiev.info", "/"),
                shareContentType
            )
        }
    }
}
