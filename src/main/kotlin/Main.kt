package org.example

import com.sun.security.auth.UserPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureAuth()
    configureRouting()
}

fun Application.configureAuth() {
    install(Authentication) {
       form("auth-form") {
           userParamName = "username"
           passwordParamName = "password"

           validate { credentials ->
               if (credentials.name == "jetbrains" && credentials.password == "foobar") {
                   UserIdPrincipal(credentials.name)
               } else {
                   null
               }
           }
           challenge {
               call.respond(HttpStatusCode.Unauthorized, "Credentials are not valid")
           }
       }
    }
}

fun Application.configureRouting() {
    routing {
        authenticate("auth-form") {
            get("/login") {
                call.respondText("Hello, ${call.principal<UserIdPrincipal>()?.name}")
            }
        }
        get("/") {
            call.respondText("Hello, world!")
        }
    }
}