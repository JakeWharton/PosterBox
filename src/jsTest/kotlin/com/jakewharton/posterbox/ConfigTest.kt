package com.jakewharton.posterbox

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class ConfigTest {
	@Ignore // TODO https://github.com/akuleshov7/ktoml/issues/94
	@Test fun emptyConfig() {
		val expected = Config(
			itemDisplayDuration = 15.seconds,
			itemTransition = ItemTransition.Fade,
			plex = null,
		)
		val actual = Config.parse("# empty")
		assertEquals(expected, actual)
	}

	@Test fun validTransitions() {
		val expectedNone = Config(itemTransition = ItemTransition.None)
		val actualNone = Config.parse("""
			|itemTransition = "none"
			|""".trimMargin())
		assertEquals(expectedNone, actualNone)

		val expectedFade = Config(itemTransition = ItemTransition.Fade)
		val actualFade = Config.parse("""
			|itemTransition = "fade"
			|""".trimMargin())
		assertEquals(expectedFade, actualFade)

		val expectedCrossfade = Config(itemTransition = ItemTransition.Crossfade)
		val actualCrossfade = Config.parse("""
			|itemTransition = "crossfade"
			|""".trimMargin())
		assertEquals(expectedCrossfade, actualCrossfade)

		val expectedSlideLeft = Config(itemTransition = ItemTransition.SlideLeft)
		val actualSlideLeft = Config.parse("""
			|itemTransition = "slide-left"
			|""".trimMargin())
		assertEquals(expectedSlideLeft, actualSlideLeft)

		val expectedSlideRight = Config(itemTransition = ItemTransition.SlideRight)
		val actualSlideRight = Config.parse("""
			|itemTransition = "slide-right"
			|""".trimMargin())
		assertEquals(expectedSlideRight, actualSlideRight)
	}

	@Test fun invalidTransitionThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parse("""
				|itemTransition = "star-wipe"
				|""".trimMargin())
		}
		assertEquals("Unknown item transition name: star-wipe", t.message)
	}

	@Test fun zeroItemDurationThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parse("""
				|itemDisplayDuration = 0
				|""".trimMargin())
		}
		assertEquals("Duration seconds must be greater than zero: 0", t.message)
	}

	@Test fun negativeItemDurationThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parse("""
				|itemDisplayDuration = -2
				|""".trimMargin())
		}
		assertEquals("Duration seconds must be greater than zero: -2", t.message)
	}

	@Test fun validDuration() {
		val expected = Config(itemDisplayDuration = 30.seconds)
		val actual = Config.parse("""
			|itemDisplayDuration = 30
			|""".trimMargin())
		assertEquals(expected, actual)
	}
}
