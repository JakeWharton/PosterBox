package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Footer
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.Object
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun PosterBox(appData: AppData) {
	// TODO handle empty list
	PosterDisplay(appData)
}

@Composable
fun PosterDisplay(appData: AppData) {
	// Ensure effects capture state which can be re-read rather than raw values. This ensures changes
	// to data does not restart the update loop and are merely reflected on next iteration instead.
	val renderSettings by rememberUpdatedState(appData.renderSettings)
	val posters by rememberUpdatedState(appData.posters)

	val posterRandomizer: PosterRandomizer = remember { WeightedHistoricalPosterRandomizer() }

	// Start with the same poster loaded in both positions. The browser should de-duplicate this
	// request ensuring it is displayed as soon as possible.
	var posterOne by remember { mutableStateOf(posterRandomizer.next(posters)) }
	var posterTwo by remember { mutableStateOf(posterOne) }
	var posterOneActive by remember { mutableStateOf(true) }

	if (posters.size > 1) {
		LaunchedEffect(Unit) {
			while (true) {
				val nextPoster = posterRandomizer.next(posters)
				if (posterOneActive) {
					posterTwo = nextPoster
				} else {
					posterOne = nextPoster
				}
				delay(renderSettings.itemDisplayDuration)

				posterOneActive = !posterOneActive
				delay(CssAnimationDuration)
			}
		}
	}

	Header {
		PosterHeader("Now Showing")
	}
	Main({ classes(transitionClass(renderSettings.itemTransition)) }) {
		PosterImage(posterOne.plexPoster, posterOne.title, posterOneActive)
		PosterImage(posterTwo.plexPoster, posterTwo.title, !posterOneActive)
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
private fun PosterImage(url: String, title: String, active: Boolean) {
	Object({
		classes(activeClass(active))
		attr("data", "${Poster.route}?path=$url")
		attr("type", "image/jpeg")
	}) {
		H1 { Text(title) }
		P { Text("Unable to load poster") }
	}
}

@Composable
private fun PosterFooter(poster: Poster, active: Boolean) {
	Div({ classes(activeClass(active)) }) {
		poster.contentRating?.let { contentRating ->
			Span({ classes(ratingClass(contentRating)) }) { Text(contentRating) }
		}
		poster.studio?.let { studio ->
			Span { Text(studio) }
		}
		Span { Text("${poster.runtime}m") }
		poster.rating?.let { rating ->
			Span { Text("$rating%") }
		}
		Span { Text(poster.year.toString()) }
	}
}

private fun activeClass(active: Boolean): String {
	return if (active) "active" else "hidden"
}

private fun ratingClass(rating: String): String {
	return when (rating.lowercase()) {
		"nr", "not rated", "unrated", "r", "tv-ma" -> "red"
		"pg-13", "tv-14" -> "orange"
		"pg", "tv-pg", "tv-y7" -> "blue"
		"g", "tv-g", "tv-y" -> "green"
		else -> "unknown"
	}
}

private fun transitionClass(itemTransition: ItemTransition) = "transition-${itemTransition.string}"
