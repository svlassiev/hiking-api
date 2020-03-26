package info.vlassiev.serg.api

import com.google.firebase.auth.FirebaseAuth
import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.image.createSignedUrl
import info.vlassiev.serg.model.ImageList
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.getOrFail
import java.util.*

fun Routing.editApi(path: String, imageClient: ImageClient, adminEmail: String): Route {
    return route(path) {
        get("/data") {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.respond(imageClient.getEditPageData())
            }
        }
        route("/images-lists") {
            post() {
                val idToken = call.parameters.getOrFail("idToken")
                if (!validToken(idToken, adminEmail)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    val imageList = call.receive<ImageList>()
                    call.respond(imageClient.addImagesList(imageList))
                }
            }
        }
        addImagesListApi(imageClient, adminEmail)
        addImageApi(imageClient, adminEmail)
    }
}

private fun Route.addImagesListApi(imageClient: ImageClient, adminEmail: String): Route {
    return route("/images-lists/{listId}") {
        put("/name") {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                val listId = call.parameters.getOrFail("listId")
                val request = call.receive<ImageClient.UpdateListNameRequest>()
                imageClient.updateImagesListName(listId, request)
                call.respond(HttpStatusCode.OK)
            }
        }
        delete() {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                val listId = call.parameters.getOrFail("listId")
                call.respond(imageClient.deleteImagesList(listId))
            }
        }
        delete("/images/{imageId}") {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                val imageId = call.parameters.getOrFail("imageId")
                val listId = call.parameters.getOrFail("listId")
                call.respond(imageClient.deleteImage(listId, imageId))
            }
        }
    }
}

private fun Route.addImageApi(imageClient: ImageClient, adminEmail: String): Route {
    return route("/images") {
        post() {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                val request = call.receive<ImageClient.AddImageRequest>()
                call.respond(imageClient.addImageFromGoogleStorage(request))
            }
        }
        post("/signed-url") {
            val idToken = call.parameters.getOrFail("idToken")
            if (!validToken(idToken, adminEmail)) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                val listId = call.receive<String>()
                val imageName = UUID.randomUUID().toString() + ".jpg"

                call.respond(createSignedUrl(listId, imageName))
            }
        }
        route("/{imageId}") {
            put("/description") {
                val idToken = call.parameters.getOrFail("idToken")
                if (!validToken(idToken, adminEmail)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    val imageId = call.parameters.getOrFail("imageId")
                    val request = call.receive<ImageClient.UpdateImageDescriptionRequest>()
                    imageClient.updateImageDescription(imageId, request)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

private fun validToken(idToken: String, adminEmail: String): Boolean {
    val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)
    return decodedToken.email == adminEmail
}