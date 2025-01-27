package org.example

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.sun.security.auth.UserPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level
import java.util.*


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
    configureLogger()
}

fun Application.configureAuth() {
    install(Authentication) {
        jwt("auth-jwt") {
//            realm = applicationEnvironment().config.propertyOrNull("jwt.realm")?.getString() ?: "default_value"
//
//            val secret = applicationEnvironment().config.propertyOrNull("jwt.secret")?.getString() ?: "default_value"
//            val issuer = applicationEnvironment().config.propertyOrNull("jwt.issuer")?.getString() ?: "default_value"

            realm = "ktor-sample"

            val secret = "my_very_secret"
            val issuer = "my_very_issuer"

            verifier(makeJwtVerifier(issuer, secret))

            validate { credential ->
                if (credential.payload.getClaim("username").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}

fun makeJwtVerifier(issuer: String, secret: String): JWTVerifier = JWT
        .require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .build()


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, world!")
        }

        post("/login") {
            val userName = call.receiveParameters()["username"] ?: return@post call.respondText("Missing username")


            val token = generateToken(userName)
            call.respondText(token)
        }

        authenticate("auth-jwt") {
            get("/secure_test") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("username")?.asString()
                call.respondText("Hello, $username! This is a protected route.")
            }
        }
    }
}

// Создание токена
fun generateToken(username: String): String {
    val algorithm = Algorithm.HMAC256("secret")
    return JWT.create()
        .withAudience("ktor-audience")
        .withIssuer("ktor-sample")
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // Токен действует неделю
        .sign(algorithm)
}

fun Application.configureLogger() {
    install(CallLogging){
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val queryParams =
                call.request.queryParameters
                    .entries()
                    .joinToString(", ") { "${it.key}=${it.value}" }
            val duration = call.processingTimeMillis()
            val remoteHost = call.request.origin.remoteHost
            val coloredStatus =
                when {
                    status == null -> "\u001B[33mUNKNOWN\u001B[0m"
                    status.value < 300 -> "\u001B[32m$status\u001B[0m"
                    status.value < 400 -> "\u001B[33m$status\u001B[0m"
                    else -> "\u001B[31m$status\u001B[0m"
                }
            val coloredMethod = "\u001B[36m$httpMethod\u001B[0m"
            """
        |
        |------------------------ Request Details ------------------------
        |Status: $coloredStatus
        |Method: $coloredMethod
        |Path: $path
        |Query Params: $queryParams
        |Remote Host: $remoteHost
        |User Agent: $userAgent
        |Duration: ${duration}ms
        |------------------------------------------------------------------
        |
  """.trimMargin()
        }
    }
}