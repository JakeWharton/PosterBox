package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders.ETag
import io.ktor.http.HttpStatusCode
import kotlinx.browser.window

interface BackendService {
	suspend fun appData(eTag: String?): AppDataResponse
}

sealed interface AppDataResponse {
	data class Success(val config: AppData, val eTag: String) : AppDataResponse
	object NotModified : AppDataResponse
	data class Error(val error: String) : AppDataResponse
}

class HttpBackendService(
	private val client: HttpClient,
) : BackendService {
	override suspend fun appData(eTag: String?): AppDataResponse {
		val response = try {
			// Explicitly specify origin to work around https://youtrack.jetbrains.com/issue/KTOR-3191.
			client.get<HttpResponse>("${window.location.origin}${AppData.route}") {
				expectSuccess = false // Allow non-200 responses.
			}
		} catch (e: Throwable) {
			return AppDataResponse.Error(e.stackTraceToString())
		}
		return when (response.status) {
			HttpStatusCode.OK -> {
				val newETag = requireNotNull(response.headers[ETag]) {
					"/config.json response did not include required ETag header"
				}
				if (newETag == eTag) {
					return AppDataResponse.NotModified
				}
				val configJson = response.readText()
				val config = AppData.decodeFromJson(configJson)
				AppDataResponse.Success(config, newETag)
			}
			else -> AppDataResponse.Error("HTTP ${response.status}")
		}
	}
}
