package com.jakewharton.posterbox

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.dom.clear
import org.jetbrains.compose.web.renderComposable

suspend fun main() {
	val body = document.body!!

	// Hide initial loading state statically defined in the HTML.
	body.clear()

	renderComposable(body) {
		val configState = remember { mutableStateOf<Config?>(null) }
		val config = configState.value

		val postersState = remember { mutableStateOf<List<Poster>?>(null) }
		val posters = postersState.value

		if (config == null) {
			LoadingConfig()
			LaunchedEffect(Unit) {
				configState.value = loadConfig()
			}
		} else {
			LaunchedEffect(Unit) {
				// TODO continuously HEAD config
			}
			LaunchedEffect(config) {
				// TODO continuously sync remote data
				postersState.value = loadPosters()
			}

			if (posters == null) {
				LoadingPosters()
			} else if (posters.isEmpty()) {
				TODO("Empty content")
			} else {
				LaunchedEffect(Unit) {
					// Prevent animations for initial content load.
					body.classList.add("disable-animation")
					// Wait for the CSS animation time before re-enabling. We use this as an approximation
					// for how long it takes the browser to render the initial DOM and load the first poster.
					delay(CssAnimationDuration)
					body.classList.remove("disable-animation")
				}
				PosterBox(config, posters)
			}
		}
	}
}

private suspend fun loadConfig(): Config {
	delay(1_000)
	return Config(
		posterDisplayDuration = 15.seconds,
		posterTransition = Transition.SlideLeft,
	)
}

private suspend fun loadPosters(): List<Poster> {
	delay(1_000)
	return listOf(
		Poster("https://www.themoviedb.org/t/p/original/fSRb7vyIP8rQpL0I47P3qUsEKX3.jpg", "G", "Who Cares", 101, 94, 1998),
		Poster("https://www.themoviedb.org/t/p/original/7aatn3TWAVo9a2OJyQTuYpoB48G.jpg", "PG", "Some Thing", 123, 64, 2020),
		Poster("https://www.themoviedb.org/t/p/original/v6Xmj8Fy7ZruVTz3y2Po7O0TQh4.jpg", "PG-13", "Not Me", 202, 88, 2008),
		Poster("https://www.themoviedb.org/t/p/original/asDqvkE66EegtKJJXIRhBJPxscr.jpg", "R", "That Person", 97, null, 1974),
		Poster("https://www.themoviedb.org/t/p/original/mpgDeLhl8HbhI03XLB7iKO6M6JE.jpg", "Whatever", "Other Stuff", 888, 43, 2015),
	)
}

val CssAnimationDuration = 1.seconds
