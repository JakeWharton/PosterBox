package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

suspend fun main() {
	val body = document.body!!

	// TODO load config
	delay(1_000)

	// TODO load data
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
		PosterBox(posters)
	}
}

enum class PosterTransition(val className: String) {
	None("transition-none"),
	Crossfade("transition-crossfade"),
	Fade("transition-fade"),
	SlideLeft("transition-slide-left"),
	SlideRight("transition-slide-right"),
	PageTurn("transition-page-turn"),
}

@Composable
private fun PosterBox(posters: List<Poster>) {
	// Start with the same poster loaded in both positions. The browser should de-duplicate this
	// request ensuring it is displayed as soon as possible.
	var posterOne by remember { mutableStateOf(posters[0]) }
	var posterTwo by remember { mutableStateOf(posters[0]) }
	var posterOneActive by remember { mutableStateOf(true) }

	LaunchedEffect(posters) {
		// Wait for the CSS animation time. CSS animations are actually disabled for the initial load,
		// but we use this as an approximation for how long it takes the browser to render the initial
		// DOM and hopefully load the first poster. After this we can re-enable animations.
		delay(1_000)
		document.body!!.classList.remove("disable-animation")

		var nextPosterIndex = 1
		while (true) {
			val nextPoster = posters[nextPosterIndex]
			if (posterOneActive) {
				posterTwo = nextPoster
			} else {
				posterOne = nextPoster
			}
			delay(3_000) // Poster display time.

			posterOneActive = !posterOneActive
			delay(1_000) // CSS animation time.

			if (++nextPosterIndex == posters.size) {
				nextPosterIndex = 0
			}
		}
	}

	Header {
		PosterHeader("Now Showing")
	}
	Main({ classes(PosterTransition.SlideLeft.className) }) {
		PosterImage(posterOne.posterUrl, posterOneActive)
		PosterImage(posterTwo.posterUrl, !posterOneActive)
	}
	Footer {
		PosterFooter(posterOne, posterOneActive)
		PosterFooter(posterTwo, !posterOneActive)
	}
}

@Composable
private fun PosterHeader(content: String) {
	Div {
		Text(content)
	}
}

@Composable
private fun PosterImage(url: String, active: Boolean) {
	Img(url) { classes(activeClass(active)) }
}

@Composable
private fun PosterFooter(poster: Poster, active: Boolean) {
	Div({ classes(activeClass(active)) }) {
		Span({ classes(ratingClass(poster.rating)) }) { Text(poster.rating) }
		Span { Text(poster.productionCompany) }
		Span { Text("${poster.length}m") }
		if (poster.score != null) {
			Span { Text("${poster.score}%") }
		}
		Span { Text(poster.year.toString()) }
	}
}

private fun activeClass(active: Boolean): String {
	return if (active) "active" else "hidden"
}

private fun ratingClass(rating: String): String {
	return when (rating.lowercase()) {
		"nr", "unrated", "r", "tv-ma" -> "red"
		"pg-13", "tv-14" -> "orange"
		"pg", "tv-pg", "tv-y7" -> "blue"
		"g", "tv-g", "tv-y" -> "green"
		else -> "unknown"
	}
}

private data class Poster(
	val posterUrl: String,
	val rating: String,
	val productionCompany: String,
	val length: Int,
	val score: Int?,
	val year: Int,
)
