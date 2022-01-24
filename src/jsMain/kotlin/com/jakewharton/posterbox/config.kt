package com.jakewharton.posterbox

import kotlin.time.Duration

data class Config(
	val posterDisplayDuration: Duration,
	val posterTransition: Transition,
)

enum class Transition {
	None,
	Crossfade,
	Fade,
	SlideLeft,
	SlideRight,
}
