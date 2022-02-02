package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface PlexService {
	suspend fun posters(): List<Poster>
	suspend fun poster(path: String): PosterImage
}

class PosterImage(
	val bytes: ByteArray,
	val contentType: ContentType?,
)

class HttpPlexService(
	private val client: HttpClient,
	private val config: Config.Plex,
) : PlexService {
	private val json = Json {
		ignoreUnknownKeys = true
	}

	override suspend fun posters(): List<Poster> {
		val sectionsUrl = URLBuilder(config.host).takeFrom("/library/sections").build()
		val sectionsResponse = client.get<HttpResponse>(sectionsUrl) {
			header(PlexToken, config.token)
			header(Accept, ContentType.Application.Json)
		}
		// TODO error handling
		val sectionsJson = sectionsResponse.readText()
		val sections = json.decodeFromString(PlexResponse.serializer(PlexSections.serializer()), sectionsJson)

		return sections.mediaContainer
			.sections
			.filter { it.type == "movie" || it.type == "show" }
			.filter { config.libraries == null || it.title in config.libraries }
			.flatMap {
				val sectionUrl = URLBuilder(config.host).takeFrom("/library/sections/${it.key}/all").build()
				val sectionResponse = client.get<HttpResponse>(sectionUrl) {
					header(PlexToken, config.token)
					header(Accept, ContentType.Application.Json)
				}
				// TODO error handling
				val sectionJson = sectionResponse.readText()
				val section = json.decodeFromString(PlexResponse.serializer(PlexItems.serializer()), sectionJson)
				section.mediaContainer.items
					.filter { item -> (item.computedRating ?: 0) >= config.minimumRating }
					.map { item ->
						Poster(
							title = item.title,
							studio = item.studio,
							runtime = item.runtimeMinutes,
							year = item.year,
							contentRating = item.contentRating,
							rating = item.computedRating,
							plexPoster = item.thumb,
						)
					}
			}
	}

	override suspend fun poster(path: String): PosterImage {
		val posterUrl = URLBuilder(config.host).takeFrom(path).build()
		val response = client.get<HttpResponse>(posterUrl) {
			header(PlexToken, config.token)
		}
		val imageBytes = response.readBytes()
		val imageContentType = response.contentType()
		return PosterImage(imageBytes, imageContentType)
	}

	private companion object {
		private const val PlexToken = "X-Plex-Token"
	}
}

@Serializable
private data class PlexResponse<T>(
	@SerialName("MediaContainer")
	val mediaContainer: T,
)

@Serializable
private data class PlexSections(
	@SerialName("Directory")
	val sections: List<SectionHeader>,
) {
	@Serializable
	data class SectionHeader(
		val key: String,
		val title: String,
		val type: String,
	)
}

@Serializable
private data class PlexItems(
	@SerialName("Metadata")
	val items: List<PlexItem>,
)

@Serializable
private data class PlexItem(
	val title: String,
	val studio: String? = null,
	@SerialName("duration")
	val durationMillis: Int,
	val contentRating: String? = null,
	val year: Int,
	val rating: Double? = null,
	val audienceRating: Double? = null,
	val thumb: String,
) {
	val computedRating: Int? = (audienceRating ?: rating)?.let { (it * 10).toInt() }
	val runtimeMinutes: Int = ((durationMillis / 1000) + 59) / 60
}
