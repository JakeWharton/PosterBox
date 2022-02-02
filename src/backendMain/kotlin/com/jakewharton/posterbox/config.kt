package com.jakewharton.posterbox

import com.akuleshov7.ktoml.Toml
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

@Serializable
data class Config(
	@Serializable(DurationSerializer::class)
	val itemDisplayDuration: Duration = 15.seconds,
	@Serializable(ItemTransitionSerializer::class)
	val itemTransition: ItemTransition = ItemTransition.Fade,
	val plex: Plex? = null,
) {
	init {
		require(itemDisplayDuration.isPositive()) {
			"Item display duration must be positive: $itemDisplayDuration"
		}
	}

	companion object {
		private val serializer = Toml

		fun parseFromToml(string: String): Config {
			return serializer.decodeFromString(serializer(), string)
		}
	}

	@Serializable
	data class Plex(
		val host: String,
		val token: String,
		val libraries: Set<String>? = null,
		val minimumRating: Long = 0,
		@Serializable(DurationSerializer::class)
		val syncIntervalDuration: Duration = 15.minutes,
	) {
		init {
			require(minimumRating in 0L..100L) {
				"Minimum rating must be in the range [0, 100]: $minimumRating"
			}
			require(syncIntervalDuration.isPositive()) {
				"Sync interval duration must be positive: $syncIntervalDuration"
			}
		}
	}
}
