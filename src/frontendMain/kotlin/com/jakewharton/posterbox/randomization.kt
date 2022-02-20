package com.jakewharton.posterbox

import kotlin.random.Random

interface PosterRandomizer {
	fun next(posters: List<Poster>): Poster
}

class WeightedHistoricalPosterRandomizer(
	private val random: Random = Random,
	private val historyCachePercentage: Float = 0.25f,
) : PosterRandomizer {
	init {
		require(historyCachePercentage >= 0f && historyCachePercentage < 1f) {
			"History cache percentage must be in range [0,1): $historyCachePercentage"
		}
	}

	private val recentlySeen = ArrayDeque<Poster>()

	override fun next(posters: List<Poster>): Poster {
		require(posters.isNotEmpty()) { "Poster list was empty" }

		while (recentlySeen.size > posters.size * historyCachePercentage) {
			recentlySeen.removeFirst()
		}

		val unseenPosters = posters - recentlySeen
		val ratingSum = unseenPosters.sumOf { it.ratingWeight }
		var skipRating = random.nextInt(ratingSum)

		var index = 0
		var nextPoster: Poster
		do {
			nextPoster = unseenPosters[index++]
			skipRating -= nextPoster.ratingWeight
		} while (skipRating >= 0)

		recentlySeen.addLast(nextPoster)

		return nextPoster
	}

	companion object {
		private const val buckets = 10

		val Poster.ratingWeight: Int
			get() {
				val boundRating = rating?.coerceIn(1, 100) ?: 1
				return ((boundRating - 1) / buckets) + 1
			}
	}
}
