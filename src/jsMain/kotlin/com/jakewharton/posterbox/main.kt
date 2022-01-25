package com.jakewharton.posterbox

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlin.time.Duration.Companion.seconds
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import org.jetbrains.compose.web.renderComposable

fun main() {
	val body = document.body!!

	// Hide static "noscript" content from HTML.
	body.clear()

	val locationUrl = Url(window.location.href)
	val configPath = locationUrl.parameters["config"] ?: "config/config.toml"
	val configUrl = URLBuilder(locationUrl).takeFrom(configPath).build()
	console.log("Resolved config URL: $configUrl")

	val client = HttpClient(Js)

	renderComposable(body) {
		when (val configState = presentConfigState(client, configUrl)) {
			is ConfigState.None -> {
				// TODO handle errors in this state
				LoadingConfig()
			}
			is ConfigState.Loaded -> {
				// TODO handle errors in this state
				PosterBox(configState.config)
			}
		}
	}
}

val CssAnimationDuration = 1.seconds
