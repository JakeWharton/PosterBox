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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpHeaders.IfNoneMatch
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.request.header
import io.ktor.response.etag
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.flattenForEach
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.time.Instant
import java.util.UUID
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.time.Duration.Companion.seconds
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
		val httpClient = HttpClient(Java)

		embeddedServer(Netty, port) {
			var state: HttpState? = null

			launch {
				var lastModified: Instant? = null
				var serverConfig: ServerConfig? = null
				while (isActive) {
					val newLastModified = configFile.getLastModifiedTime().toInstant()
					if (newLastModified != lastModified) {
						lastModified = newLastModified
						// TODO handle errors
						val newServerConfig = ServerConfig.parseFromToml(configFile.readText())
						if (newServerConfig != serverConfig) {
							serverConfig = newServerConfig
							val eTag = UUID.randomUUID().toString()

							if (newServerConfig.plex != null) {
								// TODO handle errors
								val newPosters = loadPosters(httpClient, newServerConfig.plex)
								val appData = AppData(
									renderSettings = RenderSettings(
										itemDisplayDuration = newServerConfig.itemDisplayDuration,
										itemTransition = newServerConfig.itemTransition,
									),
									posters = newPosters,
								)
								val appDataJson = appData.encodeToJson()
								state = HttpState(eTag, newServerConfig, appDataJson)
							}
						}
					}
					delay(15.seconds)
				}
			}

			routing {
				get(AppData.route) {
					@Suppress("NAME_SHADOWING") // Read once to avoid tearing state.
					val state = state
					if (state == null) {
						call.respond(HttpStatusCode(425, "Too Early"))
					} else if (call.request.header(IfNoneMatch) == state.eTag) {
						call.respond(HttpStatusCode.NotModified)
					} else {
						call.response.etag(state.eTag)
						call.respondText(state.appDataJson)
					}
				}
				get(Poster.route) {
					@Suppress("NAME_SHADOWING") // Read once to avoid tearing state.
					val state = state
					if (state != null) {
						val posterPath = checkNotNull(call.request.queryParameters["path"]) {
							"Query parameter 'path required"
						}
						val plexConfig = checkNotNull(state.serverConfig.plex) {
							"Config does not contain 'plex' section"
						}
						val posterUrl = URLBuilder(plexConfig.host).takeFrom(posterPath).build()

						val response = httpClient.get<HttpResponse>(posterUrl) {
							header("X-Plex-Token", plexConfig.token)
						}
						response.headers.flattenForEach { key, value ->
							if (!HttpHeaders.ContentType.equals(key, ignoreCase = true) &&
								  !HttpHeaders.ContentLength.equals(key, ignoreCase = true)) {
								call.response.header(key, value)
							}
						}
						call.respondBytes(contentType = response.contentType(), status = response.status) {
							response.readBytes()
						}
					} else {
						call.respond(HttpStatusCode.NotFound)
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
		val serverConfig: ServerConfig,
		val appDataJson: String,
		val errors: List<String> = emptyList(),
	)

	private companion object {
		private const val defaultPort = 9931
	}
}
