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
    val resultados: List<ResultadoMulti> = emptyList(),

    /** En que pagina va esto. TMDB devuelve 20 resultados por pagina. */
    @SerializedName("page")
    val pagina: Int = 1,

    /** Cuantas hay en total, para saber cuando dejar de pedir. */
    @SerializedName("total_pages")
    val totalDePaginas: Int = 1
)

/**
 * Un resultado de la busqueda mezclada, sea lo que sea.
 *
 * Casi todo viene opcional porque el mismo tipo tiene que valer para una pelicula, una serie y
 * una persona, y cada una trae unos campos distintos. Antes de fiarse de nada hay que mirar
 * [tipo]: es lo unico que dice de que se esta hablando. Y de vez en cuando TMDB cuela entradas
 * a medias, sin titulo o sin tipo, asi que el repositorio las tira antes de llegar a la vista.
 */
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
