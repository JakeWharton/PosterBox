package com.jakewharton.posterbox

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class ServerConfigTest {
	@Ignore // TODO https://github.com/akuleshov7/ktoml/issues/94
	@Test fun emptyServerConfig() {
		val expected = ServerConfig(
			itemDisplayDuration = 15.seconds,
			itemTransition = ItemTransition.Fade,
			plex = null,
		)
		val actual = ServerConfig.parseFromToml("# empty")
		assertEquals(expected, actual)
	}

	@Test fun validTransitions() {
		val expectedNone = ServerConfig(itemTransition = ItemTransition.None)
		val actualNone = ServerConfig.parseFromToml("""
			|itemTransition = "none"
			|""".trimMargin())
		assertEquals(expectedNone, actualNone)

		val expectedFade = ServerConfig(itemTransition = ItemTransition.Fade)
		val actualFade = ServerConfig.parseFromToml("""
			|itemTransition = "fade"
			|""".trimMargin())
		assertEquals(expectedFade, actualFade)

		val expectedCrossfade = ServerConfig(itemTransition = ItemTransition.Crossfade)
		val actualCrossfade = ServerConfig.parseFromToml("""
			|itemTransition = "crossfade"
			|""".trimMargin())
		assertEquals(expectedCrossfade, actualCrossfade)

		val expectedSlideLeft = ServerConfig(itemTransition = ItemTransition.SlideLeft)
		val actualSlideLeft = ServerConfig.parseFromToml("""
			|itemTransition = "slide-left"
			|""".trimMargin())
		assertEquals(expectedSlideLeft, actualSlideLeft)

		val expectedSlideRight = ServerConfig(itemTransition = ItemTransition.SlideRight)
		val actualSlideRight = ServerConfig.parseFromToml("""
			|itemTransition = "slide-right"
			|""".trimMargin())
		assertEquals(expectedSlideRight, actualSlideRight)
	}

	@Test fun invalidTransitionThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			ServerConfig.parseFromToml("""
				|itemTransition = "star-wipe"
				|""".trimMargin())
		}
		assertEquals("Unknown item transition name: star-wipe", t.message)
	}

	@Test fun zeroItemDurationThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			ServerConfig.parseFromToml("""
				|itemDisplayDuration = 0
				|""".trimMargin())
		}
		assertEquals("Duration seconds must be greater than zero: 0", t.message)
	}

	@Test fun negativeItemDurationThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			ServerConfig.parseFromToml("""
				|itemDisplayDuration = -2
				|""".trimMargin())
		}
		assertEquals("Duration seconds must be greater than zero: -2", t.message)
	}

	@Test fun validDuration() {
		val expected = ServerConfig(itemDisplayDuration = 30.seconds)
		val actual = ServerConfig.parseFromToml("""
			|itemDisplayDuration = 30
			|""".trimMargin())
		assertEquals(expected, actual)
	}
}
