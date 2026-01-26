package com.example.dailymovie.client.response

import com.example.dailymovie.models.VideoModel
import com.google.gson.annotations.SerializedName

/**
 * Los videos de una pelicula o de una serie.
 *
 * Vienen mezclados y sin ningun orden util: trailers, clips, escenas del rodaje. El campo
 * `type` de cada uno es lo que los separa, y el repositorio pone los trailers delante porque es
 * lo unico que la gente entra a ver.
 *
 * Casi todos son de YouTube, y la app los abre alli en vez de incrustarlos: YouTube bloquea la
 * reproduccion dentro de un WebView de Android.
 */
data class VideoResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("results")
    val results: List<VideoModel> = emptyList()
)
