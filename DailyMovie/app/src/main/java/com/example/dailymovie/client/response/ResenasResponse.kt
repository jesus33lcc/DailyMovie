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

/**
 * Una reseña escrita por un usuario de TMDB.
 *
 * El [texto] puede venir en blanco, y una tarjeta con solo el nombre del autor no aporta nada:
 * el repositorio se queda unicamente con las que tienen algo escrito.
 */
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

/**
 * Quien escribio la reseña: su nota y su avatar.
 *
 * Todo el bloque puede faltar, no solo los campos de dentro, asi que hay que llegar hasta aqui
 * con cuidado antes de dar por hecho que hay nota.
 */
data class DetallesDelAutor(
    /** La nota que le puso, sobre 10. Puede no haberla puesto. */
    @SerializedName("rating")
    val nota: Double?,

    @SerializedName("avatar_path")
    val avatar: String?
)
