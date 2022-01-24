package com.jakewharton.posterbox

import com.akuleshov7.ktoml.Toml
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Config(
	@Serializable(IntToPositiveDurationSecondsSerializer::class)
	val itemDisplayDuration: Duration = 15.seconds,
	@Serializable(StringToTransitionSerializer::class)
	val itemTransition: ItemTransition = ItemTransition.Fade,
	val plex: PlexConfig? = null,
) {
	companion object {
		fun parse(toml: String): Config {
			return Toml.decodeFromString(serializer(), toml)
		}
	}
}

enum class ItemTransition {
	None,
	Crossfade,
	Fade,
	SlideLeft,
	SlideRight,
}

@Serializable
data class PlexConfig(
	val host: String,
	val token: String,
	val libraries: List<String>? = null,
)

private object IntToPositiveDurationSecondsSerializer : KSerializer<Duration> {
	override val descriptor = PrimitiveSerialDescriptor("Duration", LONG)

	override fun deserialize(decoder: Decoder): Duration {
		val seconds = decoder.decodeLong()
		require(seconds > 0) { "Duration seconds must be greater than zero: $seconds" }
		return seconds.seconds
	}

	override fun serialize(encoder: Encoder, value: Duration) {
		error("Not implemented")
	}
}

private object StringToTransitionSerializer : KSerializer<ItemTransition> {
	override val descriptor = PrimitiveSerialDescriptor("ItemTransition", STRING)

	override fun deserialize(decoder: Decoder): ItemTransition {
		return when (val value = decoder.decodeString()) {
			"none" -> ItemTransition.None
			"crossfade" -> ItemTransition.Crossfade
			"fade" -> ItemTransition.Fade
			"slide-left" -> ItemTransition.SlideLeft
			"slide-right" -> ItemTransition.SlideRight
			else -> throw IllegalArgumentException("Unknown item transition name: $value")
		}
	}

	override fun serialize(encoder: Encoder, value: ItemTransition) {
		error("Not implemented")
	}
}
