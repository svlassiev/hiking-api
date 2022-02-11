package info.vlassiev.serg.api

import info.vlassiev.serg.image.ImageClient
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Routing.viewApi(path: String, imageClient: ImageClient): Route {
    return route(path) {
        get("/folders") {
            call.respond(imageClient.getAllNonEmptyImagesLists())
        }
        post("/images") {
            val request = call.receive<ImagesRequest>()
            call.respond(imageClient.getImages(request.imageIds, request.skip, request.limit))
        }
        get("/timeline/data") {
            call.respond(imageClient.getTimelineData())
        }
        get("/timeline/data/head") {
            call.respond(imageClient.getTimelineData(head = true, tail = false))
        }
        get("/timeline/data/tail") {
            call.respond(imageClient.getTimelineData(head = false, tail = true))
        }
    }
}

data class ImagesRequest(val imageIds: List<String>, val skip: Int, val limit: Int)