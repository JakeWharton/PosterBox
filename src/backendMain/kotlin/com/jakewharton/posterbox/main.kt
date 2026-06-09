@file:JvmName("Main")

package com.jakewharton.posterbox

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.http.HttpHeaders.IfNoneMatch
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotModified
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.resource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.header
import io.ktor.server.response.etag
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.util.UUID
import kotlin.io.path.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY

fun main(vararg args: String) {
  PosterBoxCommand(FileSystems.getDefault()).main(args)
}

private class PosterBoxCommand(
  fs: FileSystem,
) : CliktCommand(
  name = "posterbox",
  help = "HTTP server for Poster Box frontend",
) {
  private val configFile by argument("CONFIG")
    .path(fileSystem = fs)
    .help("TOML config file")

  private val port by option(metavar = "PORT")
    .int()
    .default(defaultPort)
    .help("Port for the HTTP server (default $defaultPort)")

  private val debug by option(hidden = true, envvar = "POSTERBOX_DEBUG")
    .flag()

  override fun run() {
    if (debug) {
      System.setProperty(DEFAULT_LOG_LEVEL_KEY, "DEBUG")
    }

    val config = Config.parseFromToml(configFile.readText())
    val renderSettings = RenderSettings(
      itemDisplayDuration = config.itemDisplayDuration,
      itemTransition = config.itemTransition,
    )
    val httpClient = HttpClient(Java)

    val mediaService: MediaService? = when {
      config.plex != null -> HttpPlexService(httpClient, config.plex)
      config.jellyfin != null -> HttpJellyfinService(httpClient, config.jellyfin)
      else -> null
    }

    val syncInterval = when {
      config.plex != null -> config.plex.syncIntervalDuration
      config.jellyfin != null -> config.jellyfin.syncIntervalDuration
      else -> null
    }

    embeddedServer(CIO, port) {
      var state: HttpState? = null

      if (mediaService != null && syncInterval != null) {
        log.debug("[Media] Starting sync coroutine")
        launch {
          var posters: List<Poster>? = null
          while (isActive) {
            log.debug("[Media] Performing poster sync")
            val newPosters = mediaService.posters()
            if (newPosters != posters) {
              val appData = AppData(
                gitSha = gitSha,
                renderSettings = renderSettings,
                posters = newPosters,
              )
              state = HttpState(
                eTag = UUID.randomUUID().toString(),
                appDataJson = appData.encodeToJson(),
              )
              posters = newPosters
            }
            delay(syncInterval)
          }
        }
      }

      routing {
        get(AppData.route) {
          val currentState = state
          if (currentState == null) {
            call.respond(HttpStatusCode(425, "Too Early"))
          } else if (call.request.header(IfNoneMatch) == currentState.eTag) {
            call.respond(NotModified)
          } else {
            call.response.etag(currentState.eTag)
            call.respondText(currentState.appDataJson)
          }
        }

        if (mediaService != null) {
          get(Poster.route) {
            val posterPath = checkNotNull(call.request.queryParameters["path"]) {
              "Query parameter 'path' required"
            }
            val image = mediaService.poster(posterPath)
            call.respondBytes(image.bytes, image.contentType)
          }
        }

        static {
          resource("/", "static/index.html")
          resources("static")
        }
      }
    }.start(wait = true)
  }

  private data class HttpState(
    val eTag: String,
    val appDataJson: String,
    val errors: List<String> = emptyList(),
  )

  private companion object {
    private const val defaultPort = 9931
  }
}