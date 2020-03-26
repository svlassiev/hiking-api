package info.vlassiev.serg.api

import info.vlassiev.serg.image.ImageClient
import io.ktor.application.call
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.*

fun Routing.viewApi(path: String, imageClient: ImageClient): Route {
    return route(path) {
        get("/folders") {
            call.respond(imageClient.getAllNonEmptyImagesLists())
        }
        post("/images") {
            val imageIds = call.receiveOrNull<List<String>>() ?: emptyList()
            call.respond(imageClient.findImages(imageIds))
        }
    }
}