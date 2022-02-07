package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

fun main() {
	val body = document.body!!

	// Remove static "noscript" content from HTML.
	body.clear()

	val client = HttpClient(Js)
	val backend = HttpBackendService(client)

	renderComposable(body) {
		when (val appState = presentAppState(backend)) {
			is AppState.None -> {
				// TODO handle errors in this state
				LoadingConfig()
			}
			is AppState.Loaded -> {
				LaunchedEffect(Unit) {
					// Prevent animations for initial content load.
					body.classList.add("disable-animation")
					// Wait for the CSS animation time before re-enabling. We use this as an approximation
					// for how long it takes the browser to render the initial DOM and load the first poster.
					delay(CssAnimationDuration)
					body.classList.remove("disable-animation")
				}

				// TODO handle errors in this state
				PosterBox(appState.appData)
			}
			AppState.NeedsReload -> {
				LaunchedEffect(Unit) {
					window.location.reload()
				}
			}
		}
	}
}

@Composable
fun LoadingConfig() {
	Header {
		Div {
			Text("Loading config…")
		}
	}
}

val CssAnimationDuration = 1.seconds
