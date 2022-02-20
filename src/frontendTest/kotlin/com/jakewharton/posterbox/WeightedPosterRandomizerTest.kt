package com.jakewharton.posterbox

import com.jakewharton.posterbox.WeightedHistoricalPosterRandomizer.Companion.ratingWeight
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class WeightedPosterRandomizerTest {
	@Test fun ratingWeights() {
		assertEquals(1, poster(1, null).ratingWeight)
		assertEquals(1, poster(1, 0).ratingWeight)
		assertEquals(1, poster(1, 10).ratingWeight)
		assertEquals(2, poster(1, 11).ratingWeight)
		assertEquals(2, poster(1, 20).ratingWeight)
		assertEquals(9, poster(1, 90).ratingWeight)
		assertEquals(10, poster(1, 91).ratingWeight)
		assertEquals(10, poster(1, 100).ratingWeight)

		assertEquals(1, poster(1, -1000).ratingWeight)
		assertEquals(10, poster(1, 1000).ratingWeight)
	}

	@Test fun emptyPosterListThrows() {
		val randomizer = WeightedHistoricalPosterRandomizer()
		val t = assertFailsWith<IllegalArgumentException> {
			randomizer.next(emptyList())
		}
		assertEquals("Poster list was empty", t.message)
	}

	@Test fun historyCachePercentageBounds() {
		assertFailsWith<IllegalArgumentException> {
			WeightedHistoricalPosterRandomizer(historyCachePercentage = -0.01f)
		}
		assertFailsWith<IllegalArgumentException> {
			WeightedHistoricalPosterRandomizer(historyCachePercentage = 1.01f)
		}
	}

	@Test fun statisticalAccuracy() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(0), 0f)

		val poster1 = poster(1, 10)
		val poster2 = poster(2, 30)
		val poster3 = poster(3, 60)
		val posters = listOf(poster1, poster2, poster3)

		var counts1 = 0
		var counts2 = 0
		var counts3 = 0
		val trials = 10_000
		repeat(trials) {
			when (randomizer.next(posters)) {
				poster1 -> counts1++
				poster2 -> counts2++
				poster3 -> counts3++
			}
		}

		assertEquals(.1f, counts1.toFloat() / trials, .05f)
		assertEquals(.3f, counts2.toFloat() / trials, .05f)
		assertEquals(.6f, counts3.toFloat() / trials, .05f)
	}

	@Test fun firstAndLastCanBeSelected() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(4), .5f)

		val poster1 = poster(1, 1)
		val poster2 = poster(2, 1)
		val poster3 = poster(3, 1)
		val posters = listOf(poster1, poster2, poster3)

		assertSame(poster1, randomizer.next(posters))
		assertSame(poster2, randomizer.next(posters))
		assertSame(poster3, randomizer.next(posters))
	}

	@Test fun missingAndZeroRatingStillSelected() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(4), .5f)

		val poster1 = poster(1, 1)
		val poster2 = poster(2, 0)
		val poster3 = poster(3, null)
		val posters = listOf(poster1, poster2, poster3)

		assertSame(poster1, randomizer.next(posters))
		assertSame(poster2, randomizer.next(posters))
		assertSame(poster3, randomizer.next(posters))
	}

	@Test fun historicalRejection() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(4), .25f)

		val poster1 = poster(1, 100)
		val poster2 = poster(2, 1)
		val poster3 = poster(3, 1)
		val poster4 = poster(4, 1)
		val poster5 = poster(5, 1)
		val poster6 = poster(6, 1)
		val poster7 = poster(7, 1)
		val poster8 = poster(8, 1)
		val posters = listOf(poster1, poster2, poster3, poster4, poster5, poster6, poster7, poster8)

		assertSame(poster1, randomizer.next(posters))
		assertNotSame(poster1, randomizer.next(posters))
		assertNotSame(poster1, randomizer.next(posters))
		assertSame(poster1, randomizer.next(posters))
	}

	@Test fun listGrowAdjustsHistoricalCache() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(4), .5f)

		val poster1 = poster(1, 100)
		val poster2 = poster(2, 1)
		val poster3 = poster(3, 1)
		val poster4 = poster(4, 1)
		val poster5 = poster(5, 1)
		val poster6 = poster(6, 1)
		val poster7 = poster(7, 1)
		val poster8 = poster(8, 1)
		val twoPosters = listOf(poster1, poster2)
		val eightPosters = listOf(poster1, poster2, poster3, poster4, poster5, poster6, poster7, poster8)

		assertSame(poster1, randomizer.next(twoPosters))

		assertNotSame(poster1, randomizer.next(eightPosters))
		assertNotSame(poster1, randomizer.next(eightPosters))
		assertNotSame(poster1, randomizer.next(eightPosters))
		assertNotSame(poster1, randomizer.next(eightPosters))
		assertSame(poster1, randomizer.next(eightPosters))
	}

	@Test fun listShrinkAdjustsHistoricalCache() {
		val randomizer = WeightedHistoricalPosterRandomizer(Random(0), .5f)

		val poster1 = poster(1, 100)
		val poster2 = poster(2, 1)
		val poster3 = poster(3, 1)
		val poster4 = poster(4, 1)
		val poster5 = poster(5, 1)
		val poster6 = poster(6, 1)
		val poster7 = poster(7, 1)
		val poster8 = poster(8, 1)
		val twoPosters = listOf(poster1, poster3)
		val eightPosters = listOf(poster1, poster2, poster3, poster4, poster5, poster6, poster7, poster8)

		assertSame(poster1, randomizer.next(eightPosters))
		assertSame(poster3, randomizer.next(eightPosters))
		assertSame(poster6, randomizer.next(eightPosters))
		assertSame(poster5, randomizer.next(eightPosters))

		assertSame(poster1, randomizer.next(twoPosters))
	}

	private fun poster(index: Int, rating: Int?): Poster {
		return Poster("Poster$index", null, 0, 0, null, rating, "")
	}
}
