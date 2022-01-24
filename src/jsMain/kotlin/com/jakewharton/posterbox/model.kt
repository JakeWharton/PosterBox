package com.jakewharton.posterbox

data class Poster(
	val posterUrl: String,
	val rating: String,
	val productionCompany: String,
	val length: Int,
	val score: Int?,
	val year: Int,
)
