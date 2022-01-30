package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpStatusCode
import kotlinx.browser.window

sealed interface ConfigResponse {
	data class Success(val config: ClientConfig, val eTag: String) : ConfigResponse
	object NotModified : ConfigResponse
	data class Error(val error: String) : ConfigResponse
}

suspend fun loadConfig(client: HttpClient, eTag: String?): ConfigResponse {
	// Explicitly specify origin to work around https://youtrack.jetbrains.com/issue/KTOR-3191.
	val response = client.get<HttpResponse>("${window.location.origin}/config.json") {
		expectSuccess = false // Allow non-200 responses.
	}
	return when (response.status) {
		HttpStatusCode.OK -> {
			val newETag = requireNotNull(response.headers[ETag]) {
				"/config.json response did not include required ETag header"
			}
			if (newETag == eTag) {
				return ConfigResponse.NotModified
			}
			val configJson = response.readText()
			val config = ClientConfig.decodeFromJson(configJson)
			ConfigResponse.Success(config, newETag)
		}
		else -> ConfigResponse.Error("HTTP ${response.status}")
	}
}
