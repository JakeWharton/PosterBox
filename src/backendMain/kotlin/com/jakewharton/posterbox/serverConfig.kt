package com.jakewharton.posterbox

import com.akuleshov7.ktoml.Toml
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
	@Serializable(PositiveDurationSecondsSerializer::class)
	val itemDisplayDuration: Duration = 15.seconds,
	@Serializable(ItemTransitionSerializer::class)
	val itemTransition: ItemTransition = ItemTransition.Fade,
	val plex: PlexConfig? = null,
) {
	companion object {
		private val serializer = Toml

		fun parseFromToml(string: String): ServerConfig {
			return serializer.decodeFromString(serializer(), string)
		}
	}
}

@Serializable
data class PlexConfig(
	val host: String,
	val token: String,
	val libraries: Set<String>? = null,
)