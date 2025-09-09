package com.example.dailymovie.client.response

import com.example.dailymovie.models.GenreModel
import com.google.gson.annotations.SerializedName

/** La ficha de la persona llega con la misma forma que PersonModel. */
typealias PersonResponse = com.example.dailymovie.models.PersonModel

/**
 * Filmografia de una persona.
 *
 * TMDB la parte en dos: "cast" es en lo que ha actuado y "crew" en lo que ha trabajado
 * detras de la camara. Un director aparece en "crew" con job = "Director".
 */
data class PersonCreditsResponse(
    @SerializedName("cast")
    val actuaciones: List<PeliculaDePersona>,

    @SerializedName("crew")
    val trabajos: List<PeliculaDePersona>
)

data class PeliculaDePersona(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val titulo: String?,

    @SerializedName("release_date")
    val estreno: String?,

    @SerializedName("vote_average")
    val valoracion: Double,

    @SerializedName("poster_path")
    val poster: String?,

    /** El personaje que interpreta, si viene de "cast". */
    @SerializedName("character")
    val personaje: String?,

    /** El puesto que ocupa, si viene de "crew": Director, Writer... */
    @SerializedName("job")
    val puesto: String?
)

/** Lista de generos de TMDB, para el onboarding y la recomendacion. */
data class GenresResponse(
    @SerializedName("genres")
    val generos: List<GenreModel>
)
