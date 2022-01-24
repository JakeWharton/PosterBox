package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun PosterBox(config: Config, posters: List<Poster>) {
	// Start with the same poster loaded in both positions. The browser should de-duplicate this
	// request ensuring it is displayed as soon as possible.
	var posterOne by remember { mutableStateOf(posters[0]) }
	var posterTwo by remember { mutableStateOf(posters[0]) }
	var posterOneActive by remember { mutableStateOf(true) }

	if (posters.size > 1) {
		LaunchedEffect(config, posters) {
			var nextPosterIndex = 1
			while (true) {
				val nextPoster = posters[nextPosterIndex]
				if (posterOneActive) {
					posterTwo = nextPoster
				} else {
					posterOne = nextPoster
				}
				delay(config.itemDisplayDuration)

				posterOneActive = !posterOneActive
				delay(CssAnimationDuration)

				if (++nextPosterIndex == posters.size) {
					nextPosterIndex = 0
				}
			}
		}
	}

	Header {
		PosterHeader("Now Showing")
	}
	Main({ classes(transitionClass(config.itemTransition)) }) {
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

private fun transitionClass(itemTransition: ItemTransition): String {
	return when (itemTransition) {
		ItemTransition.None -> "transition-none"
		ItemTransition.Crossfade -> "transition-crossfade"
		ItemTransition.Fade -> "transition-fade"
		ItemTransition.SlideLeft -> "transition-slide-left"
		ItemTransition.SlideRight -> "transition-slide-right"
	}
}
