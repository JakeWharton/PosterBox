package com.jakewharton.posterbox

import com.jakewharton.posterbox.Transition.SlideLeft
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.dom.clear
import org.jetbrains.compose.web.renderComposable

suspend fun main() {
	val body = document.body!!

	// TODO load remote config
	delay(1_000)
	val config = Config(
		posterDisplayDuration = 15.seconds,
		posterTransition = SlideLeft,
	)

	// TODO load remote data
	delay(1_000)
	val posters = listOf(
		Poster("https://www.themoviedb.org/t/p/original/fSRb7vyIP8rQpL0I47P3qUsEKX3.jpg", "G", "Who Cares", 101, 94, 1998),
		Poster("https://www.themoviedb.org/t/p/original/7aatn3TWAVo9a2OJyQTuYpoB48G.jpg", "PG", "Some Thing", 123, 64, 2020),
		Poster("https://www.themoviedb.org/t/p/original/v6Xmj8Fy7ZruVTz3y2Po7O0TQh4.jpg", "PG-13", "Not Me", 202, 88, 2008),
		Poster("https://www.themoviedb.org/t/p/original/asDqvkE66EegtKJJXIRhBJPxscr.jpg", "R", "That Person", 97, null, 1974),
		Poster("https://www.themoviedb.org/t/p/original/mpgDeLhl8HbhI03XLB7iKO6M6JE.jpg", "Whatever", "Other Stuff", 888, 43, 2015),
	)

	// Hide static initial loading state.
	body.clear()

	renderComposable(body) {
		PosterBox(config, posters)
	}
}
