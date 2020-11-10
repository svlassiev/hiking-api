package info.vlassiev.serg.api

import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.model.VariantName.V1024
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
            call.respond(imageClient.findImages(request.imageIds, request.skip, request.limit)
                .map { it.copy(location = it.variants.find { v -> v.name == V1024 }?.location ?: it.location) })
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