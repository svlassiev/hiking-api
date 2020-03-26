package info.vlassiev.serg

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.typesafe.config.ConfigFactory
import info.vlassiev.serg.api.editApi
import info.vlassiev.serg.api.viewApi
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
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(CallLogging) { level = Level.INFO }

    install(ContentNegotiation) { gson {} }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        header(HttpHeaders.ContentType)
        anyHost()
    }

    val config = ConfigFactory.load()
    val connectionString = config.getString("mongoConnectionString")
    val adminEmail = config.getString("adminEmail")
    val firebaseUrl = config.getString("firebaseUrl")

    val repository = Repository(connectionString)
    val imageClient = ImageClient(repository)

    val firebaseOptions = FirebaseOptions.Builder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .setDatabaseUrl(firebaseUrl)
        .build()

    FirebaseApp.initializeApp(firebaseOptions)

    routing {
        get("/") {
            call.respond("healthy")
        }
        viewApi("/hiking-api", imageClient)
        editApi("/hiking-api/edit", imageClient, adminEmail)
    }
}