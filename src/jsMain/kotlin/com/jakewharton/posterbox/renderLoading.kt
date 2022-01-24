package com.jakewharton.posterbox

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Text

@Composable
fun LoadingConfig() = LoadingMessage("Loading config…")

@Composable
fun LoadingPosters() = LoadingMessage("Loading posters…")

@Composable
private fun LoadingMessage(message: String) {
	Header {
		Div {
			Text(message)
		}
	}
}
