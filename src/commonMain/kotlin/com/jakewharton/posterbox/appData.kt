package com.jakewharton.posterbox

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable
data class AppData(
	val renderSettings: RenderSettings,
	val posters: List<Poster>,
) {
	fun encodeToJson(): String {
		return serializer.encodeToString(serializer(), this)
	}

	companion object {
		private val serializer = Json
		const val route = "/data.json"

		fun decodeFromJson(string: String): AppData {
			return serializer.decodeFromString(serializer(), string)
		}
	}
}

@Serializable
data class RenderSettings(
	@Serializable(DurationSecondsSerializer::class)
	val itemDisplayDuration: Duration,
	@Serializable(ItemTransitionSerializer::class)
	val itemTransition: ItemTransition,
)

@Serializable
data class Poster(
	val title: String,
	val studio: String? = null,
	/** In minutes */
	val runtime: Int,
	val year: Int,
	val contentRating: String? = null,
	/** Range `[0,100]` */
	val rating: Int? = null,
	val plexPoster: String,
) {
	companion object {
		const val route = "/plexPoster"
	}
}

enum class ItemTransition(val string: String) {
	None("none"),
	Crossfade("crossfade"),
	Fade("fade"),
	SlideLeft("slide-left"),
	SlideRight("slide-right"),
}

object DurationSecondsSerializer : KSerializer<Duration> {
	override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

	override fun deserialize(decoder: Decoder): Duration {
		return decoder.decodeLong().seconds
	}

	override fun serialize(encoder: Encoder, value: Duration) {
		encoder.encodeLong(value.inWholeSeconds)
	}
}

object ItemTransitionSerializer : KSerializer<ItemTransition> {
	override val descriptor = PrimitiveSerialDescriptor("ItemTransition", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): ItemTransition {
		val value = decoder.decodeString()
		ItemTransition.values().forEach {
			if (value == it.string) {
				return it
			}
		}
		throw IllegalArgumentException("Unknown item transition name: $value")
	}

	override fun serialize(encoder: Encoder, value: ItemTransition) {
		encoder.encodeString(value.string)
	}
}
