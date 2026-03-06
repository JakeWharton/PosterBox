package com.jakewharton.posterbox

import io.ktor.http.ContentType

interface MediaService {
  suspend fun posters(): List<Poster>
  suspend fun poster(path: String): PosterImage
}

class PosterImage(
  val bytes: ByteArray,
  val contentType: ContentType?,
)