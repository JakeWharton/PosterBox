package com.jakewharton.posterbox

import com.akuleshov7.ktoml.exceptions.KtomlException
import com.jakewharton.posterbox.Config.Plex
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConfigTest {
	@Ignore // TODO https://github.com/akuleshov7/ktoml/issues/94
	@Test fun emptyConfig() {
		val expected = Config(
			itemDisplayDuration = 15.seconds,
			itemTransition = ItemTransition.Fade,
			plex = null,
		)
		val actual = Config.parseFromToml("# empty")
		assertEquals(expected, actual)
	}

	@Test fun itemTransitionValid() {
		val expectedNone = Config(itemTransition = ItemTransition.None)
		val actualNone = Config.parseFromToml("""
			|itemTransition = "none"
			|""".trimMargin())
		assertEquals(expectedNone, actualNone)

		val expectedFade = Config(itemTransition = ItemTransition.Fade)
		val actualFade = Config.parseFromToml("""
			|itemTransition = "fade"
			|""".trimMargin())
		assertEquals(expectedFade, actualFade)

		val expectedCrossfade = Config(itemTransition = ItemTransition.Crossfade)
		val actualCrossfade = Config.parseFromToml("""
			|itemTransition = "crossfade"
			|""".trimMargin())
		assertEquals(expectedCrossfade, actualCrossfade)

		val expectedSlideLeft = Config(itemTransition = ItemTransition.SlideLeft)
		val actualSlideLeft = Config.parseFromToml("""
			|itemTransition = "slide-left"
			|""".trimMargin())
		assertEquals(expectedSlideLeft, actualSlideLeft)

		val expectedSlideRight = Config(itemTransition = ItemTransition.SlideRight)
		val actualSlideRight = Config.parseFromToml("""
			|itemTransition = "slide-right"
			|""".trimMargin())
		assertEquals(expectedSlideRight, actualSlideRight)
	}

	@Test fun itemTransitionInvalidThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|itemTransition = "star-wipe"
				|""".trimMargin())
		}
		assertEquals("Unknown item transition name: star-wipe", t.message)
	}

	@Test fun itemDisplayDurationZeroThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|itemDisplayDuration = "PT0S"
				|""".trimMargin())
		}
		assertEquals("Item display duration must be positive: 0s", t.message)
	}

	@Test fun itemDisplayDurationNegativeThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|itemDisplayDuration = "-PT2S"
				|""".trimMargin())
		}
		assertEquals("Item display duration must be positive: -2s", t.message)
	}

	@Test fun itemDisplayDurationBadFormatThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|itemDisplayDuration = "5m10s"
				|""".trimMargin())
		}
		assertEquals("Invalid ISO duration string format: '5m10s'.", t.message)
	}

	@Test fun itemDisplayDurationValid() {
		val expected = Config(itemDisplayDuration = 30.seconds)
		val actual = Config.parseFromToml("""
			|itemDisplayDuration = "PT30S"
			|""".trimMargin())
		assertEquals(expected, actual)
	}

	@Test fun plexHostMissingThrows() {
		val t = assertFailsWith<KtomlException> {
			Config.parseFromToml("""
			|[plex]
			|token = "abc123"
			|""".trimMargin())
		}
		assertTrue("Missing the required field <host>" in t.message!!)
	}

	@Test fun plexTokenMissingThrows() {
		val t = assertFailsWith<KtomlException> {
			Config.parseFromToml("""
			|[plex]
			|host = "http://example.com"
			|""".trimMargin())
		}
		assertTrue("Missing the required field <token>" in t.message!!)
	}

	@Test fun plexConfigMinimalValid() {
		val expected = Config(plex = Plex("http://example.com", "abc123"))
		val actual = Config.parseFromToml("""
			|[plex]
			|host = "http://example.com"
			|token = "abc123"
			|""".trimMargin())
		assertEquals(expected, actual)
	}

	@Test fun minimumRatingTooLowThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|minimumRating = -2
				|""".trimMargin())
		}
		assertEquals("Minimum rating must be in the range [0, 100]: -2", t.message)
	}

	@Test fun minimumRatingTooHighThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|minimumRating = 102
				|""".trimMargin())
		}
		assertEquals("Minimum rating must be in the range [0, 100]: 102", t.message)
	}

	@Test fun minimumRatingValid() {
		val expected = Config(plex = Plex("http://example.com", "abc123", minimumRating = 40))
		val actual = Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|minimumRating = 40
				|""".trimMargin())
		assertEquals(expected, actual)
	}

	@Test fun syncIntervalZeroThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|syncIntervalDuration = "PT0S"
				|""".trimMargin())
		}
		assertEquals("Sync interval duration must be positive: 0s", t.message)
	}

	@Test fun syncIntervalNegativeThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|syncIntervalDuration = "-PT2S"
				|""".trimMargin())
		}
		assertEquals("Sync interval duration must be positive: -2s", t.message)
	}

	@Test fun syncIntervalBadFormatThrows() {
		val t = assertFailsWith<IllegalArgumentException> {
			Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|syncIntervalDuration = "5m10s"
				|""".trimMargin())
		}
		assertEquals("Invalid ISO duration string format: '5m10s'.", t.message)
	}

	@Test fun syncIntervalDurationValid() {
		val expected =
			Config(plex = Plex("http://example.com", "abc123", syncIntervalDuration = 10.minutes))
		val actual = Config.parseFromToml("""
				|[plex]
				|host = "http://example.com"
				|token = "abc123"
				|syncIntervalDuration = "PT10M"
				|""".trimMargin())
		assertEquals(expected, actual)
	}
}
