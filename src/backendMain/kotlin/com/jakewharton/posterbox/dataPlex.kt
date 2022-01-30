package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
	ignoreUnknownKeys = true
}

suspend fun loadPosters(client: HttpClient, config: PlexConfig): List<Poster> {
	val sectionsUrl = URLBuilder(config.host).takeFrom("/library/sections").build()
	val sectionsResponse = client.get<HttpResponse>(sectionsUrl) {
		header("X-Plex-Token", config.token)
		header("Accept", "application/json")
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
				header("X-Plex-Token", config.token)
				header("Accept", "application/json")
			}
			// TODO error handling
			val sectionJson = sectionResponse.readText()
			val section = json.decodeFromString(PlexResponse.serializer(PlexItems.serializer()), sectionJson)
			section.mediaContainer.items.map { item ->
				Poster(
					studio = item.studio,
					runtime = ((item.duration / 1000) + 59) / 60,
					year = item.year,
					contentRating = item.contentRating,
					rating = (item.rating ?: item.audienceRating)?.let { (it * 10).toInt() },
					plexPoster = item.thumb,
				)
			}
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
	val duration: Int,
	val contentRating: String? = null,
	val year: Int,
	val rating: Double? = null,
	val audienceRating: Double? = null,
	val thumb: String,
)
