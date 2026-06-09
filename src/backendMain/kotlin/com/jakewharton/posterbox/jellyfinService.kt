package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpJellyfinService(
  private val client: HttpClient,
  private val config: Config.Jellyfin,
) : MediaService {
  private val json = Json { ignoreUnknownKeys = true }

  override suspend fun posters(): List<Poster> {
    val librariesUrl = URLBuilder(config.host)
      .takeFrom("/Library/MediaFolders")
      .build()

    val librariesResponse = client.get(librariesUrl) {
      header(JellyfinToken, config.token)
      header(Accept, ContentType.Application.Json)
    }

    val librariesJson = librariesResponse.bodyAsText()
    val libraries = json.decodeFromString(
      JellyfinItemsResponse.serializer(),
      librariesJson,
    )

    return libraries.items
      .filter { it.collectionType == "movies" || it.collectionType == "tvshows" }
      .filter { config.libraries == null || it.name in config.libraries }
      .flatMap { library ->
        val libraryId = library.id ?: return@flatMap emptyList()

        val itemsUrl = URLBuilder(config.host)
          .takeFrom("/Items")
          .apply {
            parameters.append("ParentId", libraryId)
            parameters.append("Recursive", "true")
            parameters.append("IncludeItemTypes", "Movie,Series")
            parameters.append(
              "Fields",
              "CommunityRating,OfficialRating,RunTimeTicks,ProductionYear,Studios"
            )
          }
          .build()

        val itemsResponse = client.get(itemsUrl) {
          header(JellyfinToken, config.token)
          header(Accept, ContentType.Application.Json)
        }

        val itemsJson = itemsResponse.bodyAsText()
        val items = json.decodeFromString(
          JellyfinItemsResponse.serializer(),
          itemsJson,
        )

        items.items
          .filter { item -> (item.computedRating ?: 0) >= config.minimumRating }
          .mapNotNull { item ->
            val itemId = item.id ?: return@mapNotNull null
            val itemName = item.name ?: return@mapNotNull null

            Poster(
              title = itemName,
              studio = item.studios?.firstOrNull()?.name,
              runtime = item.runtimeMinutes ?: 0,
              year = item.productionYear ?: 0,
              contentRating = item.officialRating,
              rating = item.computedRating,
              imagePath = "/Items/$itemId/Images/Primary",
            )
          }
      }
  }

  override suspend fun poster(path: String): PosterImage {
    val posterUrl = URLBuilder(config.host).takeFrom(path).build()
    val response = client.get(posterUrl) {
      header(JellyfinToken, config.token)
    }
    return PosterImage(
      bytes = response.readBytes(),
      contentType = response.contentType(),
    )
  }

  private companion object {
    private const val JellyfinToken = "X-MediaBrowser-Token"
  }
}

@Serializable
private data class JellyfinItemsResponse(
  @SerialName("Items") val items: List<JellyfinItem> = emptyList(),
)

@Serializable
private data class JellyfinItem(
  @SerialName("Id") val id: String? = null,
  @SerialName("Name") val name: String? = null,
  @SerialName("CollectionType") val collectionType: String? = null,
  @SerialName("ProductionYear") val productionYear: Int? = null,
  @SerialName("OfficialRating") val officialRating: String? = null,
  @SerialName("CommunityRating") val communityRating: Double? = null,
  @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
  @SerialName("Studios") val studios: List<JellyfinStudio>? = null,
) {
  val computedRating: Int?
    get() = communityRating?.let { (it * 10).toInt() }

  val runtimeMinutes: Int?
    get() = runTimeTicks?.let { (((it / 10_000_000L) + 59) / 60).toInt() }
}

@Serializable
private data class JellyfinStudio(
  @SerialName("Name") val name: String? = null,
)