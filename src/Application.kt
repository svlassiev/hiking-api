package info.vlassiev.serg

import info.vlassiev.serg.model.getFolders
import info.vlassiev.serg.repository.spinUp
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import io.ktor.routing.get
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
        anyHost()
    }

    routing {
        get("/") {
            call.respond("healthy")
        }
        route("/hiking-api") {
            get("/folders") {
                call.respond(getFolders())
            }
        }
    }

    Thread {
        Thread.sleep(30_000)
        spinUp()
    }.start()
}

