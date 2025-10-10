package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta search/multi.
 *
 * Es el unico endpoint de TMDB que devuelve peliculas, series y gente juntas, asi que todos
 * los campos vienen opcionales: una pelicula trae "title" y "release_date", una serie "name" y
 * "first_air_date", y una persona "name" y "profile_path". Quien dice que es cada cosa es
 * "media_type".
 */
data class BusquedaMultiResponse(
    @SerializedName("results")
    val resultados: List<ResultadoMulti> = emptyList()
)

data class ResultadoMulti(
    @SerializedName("id")
    val id: Int,

    @SerializedName("media_type")
    val tipo: String?,

    // Peliculas
    @SerializedName("title")
    val titulo: String?,

    @SerializedName("release_date")
    val estreno: String?,

    // Series y personas
    @SerializedName("name")
    val nombre: String?,

    @SerializedName("first_air_date")
    val primeraEmision: String?,

    @SerializedName("poster_path")
    val cartel: String?,

    // Personas
    @SerializedName("profile_path")
    val foto: String?,

    @SerializedName("known_for_department")
    val oficio: String?,

    @SerializedName("vote_average")
    val nota: Double?,

    @SerializedName("popularity")
    val popularidad: Double?
)

/**
 * Lo que contesta trending.
 *
 * Trae la misma mezcla que search/multi y con los mismos campos, asi que se reaprovecha el
 * mismo tipo en vez de duplicarlo.
 */
data class TendenciasResponse(
    @SerializedName("results")
    val resultados: List<ResultadoMulti> = emptyList()
)
