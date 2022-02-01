package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun presentAppState(client: HttpClient): AppState {
	var appState by remember { mutableStateOf<AppState>(AppState.None()) }

	LaunchedEffect(Unit) {
		var eTag: String? = null
		while (isActive) {
			when (val appDataResponse = loadAppData(client, eTag)) {
				is AppDataResponse.Success -> {
					val newAppState = AppState.Loaded(
						appData = appDataResponse.config,
					)
					console.log("Loaded new config! ${appDataResponse.eTag}")
					appState = newAppState
					eTag = appDataResponse.eTag
				}
				is AppDataResponse.NotModified -> {
					// Nothing to do!
				}
				is AppDataResponse.Error -> {
					console.log("Unable to load config. ${appDataResponse.error}")
					appState = when (val oldAppState = appState) {
						is AppState.None -> oldAppState.copy(error = appDataResponse.error)
						is AppState.Loaded -> oldAppState.copy(error = appDataResponse.error)
					}
				}
			}

			delay(15.seconds)
		}
	}

	return appState
}

sealed interface AppState {
	val error: String?

	data class None(
		override val error: String? = null,
	) : AppState

	data class Loaded(
		val appData: AppData,
		override val error: String? = null,
	) : AppState
}