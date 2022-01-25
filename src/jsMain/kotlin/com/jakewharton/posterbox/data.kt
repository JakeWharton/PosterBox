package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.delay

sealed interface ConfigResponse {
	data class Success(val lastModified: String?, val config: Config) : ConfigResponse
	object NotModified : ConfigResponse
	data class Error(val error: String) : ConfigResponse
}

suspend fun loadConfig(client: HttpClient, configUrl: Url, lastModified: String?): ConfigResponse {
	val response = client.get<HttpResponse>(configUrl) {
		expectSuccess = false // Allow 304 and non-200 responses.

		if (lastModified != null) {
			header("If-Modified-Since", lastModified)
		}
	}
	return when (response.status) {
		HttpStatusCode.NotModified -> ConfigResponse.NotModified
		HttpStatusCode.OK -> {
			val configLastModified = response.headers["Last-Modified"]
			val configString = response.readText()
			val config = Config.parse(configString)
			ConfigResponse.Success(configLastModified, config)
		}
		else -> ConfigResponse.Error("HTTP ${response.status}")
	}
}

suspend fun loadPosters(): List<Poster> {
	delay(1_000)
	return listOf(
		Poster("https://www.themoviedb.org/t/p/original/fSRb7vyIP8rQpL0I47P3qUsEKX3.jpg", "G", "Who Cares", 101, 94, 1998),
		Poster("https://www.themoviedb.org/t/p/original/7aatn3TWAVo9a2OJyQTuYpoB48G.jpg", "PG", "Some Thing", 123, 64, 2020),
		Poster("https://www.themoviedb.org/t/p/original/v6Xmj8Fy7ZruVTz3y2Po7O0TQh4.jpg", "PG-13", "Not Me", 202, 88, 2008),
		Poster("https://www.themoviedb.org/t/p/original/asDqvkE66EegtKJJXIRhBJPxscr.jpg", "R", "That Person", 97, null, 1974),
		Poster("https://www.themoviedb.org/t/p/original/mpgDeLhl8HbhI03XLB7iKO6M6JE.jpg", "Whatever", "Other Stuff", 888, 43, 2015),
	)
}
