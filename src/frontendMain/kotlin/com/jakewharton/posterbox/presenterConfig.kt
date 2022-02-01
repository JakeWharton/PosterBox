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
fun presentConfigState(client: HttpClient): ConfigState {
	var config by remember { mutableStateOf<ConfigState>(ConfigState.None()) }

	LaunchedEffect(Unit) {
		var eTag: String? = null
		while (isActive) {
			when (val configResponse = loadConfig(client, eTag)) {
				is ConfigResponse.Success -> {
					val newConfigState = ConfigState.Loaded(
						config = configResponse.config,
					)
					console.log("Loaded new config! ${configResponse.eTag}")
					config = newConfigState
					eTag = configResponse.eTag
				}
				is ConfigResponse.NotModified -> {
					// Nothing to do!
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
		val config: ClientConfig,
		override val error: String? = null,
	) : ConfigState
}
