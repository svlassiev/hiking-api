package info.vlassiev.serg

import com.typesafe.config.ConfigFactory
import info.vlassiev.serg.image.ImageClient
import info.vlassiev.serg.repository.Repository
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module() {
    install(CallLogging) { level = Level.INFO }

    install(ContentNegotiation) { gson {} }

    install(CORS) {
        method(HttpMethod.Options)
        header(HttpHeaders.ContentType)
        anyHost()
    }

    val config = ConfigFactory.load()
    val connectionString = config.getString("mongoConnectionString")

    val repository = Repository(connectionString)
    val imageClient = ImageClient(repository)

    routing {
        get("/") {
            call.respond("healthy")
        }
        route("/hiking-api") {
            get("/folders") {
                call.respond(imageClient.getAllImageLists())
            }
            post("/images") {
                val imageIds = call.receiveOrNull<List<String>>() ?: emptyList()
                call.respond(imageClient.findImages(imageIds))
            }
        }
    }

//    Thread {
//        Thread.sleep(10_000)
//        spinUpDeleteWrongData(repository)
//        spinUpGoogleapisFolder(repository)
//    }.start()
}

