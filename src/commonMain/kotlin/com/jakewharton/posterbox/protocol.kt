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
data class ClientConfig(
	@Serializable(PositiveDurationSecondsSerializer::class)
	val itemDisplayDuration: Duration = 15.seconds,
	@Serializable(ItemTransitionSerializer::class)
	val itemTransition: ItemTransition = ItemTransition.Fade,
	val posters: List<Poster>,
) {
	fun encodeToJson(): String {
		return serializer.encodeToString(serializer(), this)
	}

	companion object {
		private val serializer = Json

		fun decodeFromJson(string: String): ClientConfig {
			return serializer.decodeFromString(serializer(), string)
		}
	}
}

@Serializable
data class Poster(
	val title: String,
	val studio: String? = null,
	val runtime: Int,
	val year: Int,
	val contentRating: String? = null,
	val rating: Int? = null,
	val plexPoster: String,
)

enum class ItemTransition(val string: String) {
	None("none"),
	Crossfade("crossfade"),
	Fade("fade"),
	SlideLeft("slide-left"),
	SlideRight("slide-right"),
}

object PositiveDurationSecondsSerializer : KSerializer<Duration> {
	override val descriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.LONG)

	override fun deserialize(decoder: Decoder): Duration {
		val seconds = decoder.decodeLong()
		require(seconds > 0) { "Duration seconds must be greater than zero: $seconds" }
		return seconds.seconds
	}

	override fun serialize(encoder: Encoder, value: Duration) {
		require(value.isPositive()) { "Duration must be positive: $value" }
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
