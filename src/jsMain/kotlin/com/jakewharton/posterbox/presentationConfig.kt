package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun presentConfigState(client: HttpClient, configUrl: Url): ConfigState {
	var config by remember { mutableStateOf<ConfigState>(ConfigState.None()) }

	LaunchedEffect(configUrl) {
		while (isActive) {
			val lastModified = (config as? ConfigState.Loaded)?.lastModified

			when (val configResponse = loadConfig(client, configUrl, lastModified)) {
				is ConfigResponse.Success -> {
					val newConfigState = ConfigState.Loaded(
						lastModified = configResponse.lastModified,
						config = configResponse.config,
					)
					console.log("Loaded new config! $newConfigState")
					config = newConfigState
				}
				ConfigResponse.NotModified -> {
					console.log("Config has not changed.")
				}
				is ConfigResponse.Error -> {
					console.log("Unable to load config. ${configResponse.error}")
					config = when (val oldConfig = config) {
						is ConfigState.None -> oldConfig.copy(error = configResponse.error)
						is ConfigState.Loaded -> oldConfig.copy(error = configResponse.error)
					}
				}
			}

			val refresh = (config as? ConfigState.Loaded)?.config?.itemDisplayDuration ?: 15.seconds
			delay(refresh)
		}
	}

	return config
}

sealed interface ConfigState {
	val error: String?

	data class None(
		override val error: String? = null,
	) : ConfigState

	data class Loaded(
		val lastModified: String?,
		val config: Config,
		override val error: String? = null,
	) : ConfigState
}
