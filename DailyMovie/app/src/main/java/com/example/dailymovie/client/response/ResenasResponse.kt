package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta movie/{id}/reviews.
 *
 * Son reseñas escritas por gente de TMDB, no criticas profesionales. Vienen casi siempre en
 * ingles aunque se pida el idioma local, porque es quien las escribe.
 */
data class ResenasResponse(
    @SerializedName("results")
    val resultados: List<ResenaResponse> = emptyList()
)

data class ResenaResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("author")
    val autor: String,

    @SerializedName("content")
    val texto: String,

    @SerializedName("created_at")
    val fecha: String?,

    @SerializedName("author_details")
    val detallesDelAutor: DetallesDelAutor?
)

data class DetallesDelAutor(
    /** La nota que le puso, sobre 10. Puede no haberla puesto. */
    @SerializedName("rating")
    val nota: Double?,

    @SerializedName("avatar_path")
    val avatar: String?
)
