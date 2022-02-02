@file:JvmName("Main")
package com.jakewharton.posterbox

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.http.HttpHeaders.IfNoneMatch
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotModified
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.header
import io.ktor.response.etag
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.util.UUID
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun main(vararg args: String) {
	PosterBoxCommand(FileSystems.getDefault())
		.main(args)
}

private class PosterBoxCommand(
	fs: FileSystem,
) : CliktCommand(
	name = "poster-box",
	help = "HTTP server for Poster Box frontend",
) {
	private val configFile by option(metavar = "FILE")
		.path(fileSystem = fs)
		.help("TOML config file")
		.required()
	private val port by option(metavar = "PORT")
		.int()
		.default(defaultPort)
		.help("Port for the HTTP server (default $defaultPort)")

	override fun run() {
		val config = Config.parseFromToml(configFile.readText())
		val renderSettings = RenderSettings(
			itemDisplayDuration = config.itemDisplayDuration,
			itemTransition = config.itemTransition,
		)

		val httpClient = HttpClient(Java)
		val plexService = config.plex?.let { HttpPlexService(httpClient, it) }

		embeddedServer(Netty, port) {
			var state: HttpState? = null

			if (plexService != null) {
				launch {
					var posters: List<Poster>? = null
					while (isActive) {
						// TODO handle errors
						val newPosters = plexService.posters()
						if (newPosters != posters) {
							val appData = AppData(
								renderSettings = renderSettings,
								posters = newPosters,
							)
							state = HttpState(
								eTag = UUID.randomUUID().toString(),
								appDataJson = appData.encodeToJson(),
							)
							posters = newPosters
						}
						delay(15.minutes)
					}
				}
			}

			routing {
				get(AppData.route) {
					@Suppress("NAME_SHADOWING") // Read once to avoid tearing state.
					val state = state
					if (state == null) {
						call.respond(HttpStatusCode(425, "Too Early"))
					} else if (call.request.header(IfNoneMatch) == state.eTag) {
						call.respond(NotModified)
					} else {
						call.response.etag(state.eTag)
						call.respondText(state.appDataJson)
					}
				}

				if (plexService != null) {
					get(Poster.route) {
						val posterPath = checkNotNull(call.request.queryParameters["path"]) {
							"Query parameter 'path required"
						}
						// TODO how to handle/propagate errors?
						val image = plexService.poster(posterPath)
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
