package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Serie en su version corta, la que se enseña en las listas.
 *
 * TMDB usa nombres distintos para series y peliculas aunque el dato sea el mismo: aqui
 * "name" y "first_air_date" son el titulo y la fecha de estreno.
 *
 * @property estreno cuando se emitio el primer episodio, como "1999-01-10". Puede ser null en
 *   las series anunciadas que todavia no tienen fecha.
 * @property valoracion la nota de 0 a 10; un 0 suele ser que no la ha votado nadie.
 * @property poster ruta del cartel, o null si no hay.
 */
@Parcelize
data class SerieModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val titulo: String,

    @SerializedName("first_air_date")
    val estreno: String?,

    @SerializedName("vote_average")
    val valoracion: Double,

    @SerializedName("poster_path")
    val poster: String?
) : Parcelable

/**
 * Una temporada, tal y como aparece en la ficha de la serie.
 *
 * @property numero el que se enseña en el boton ("T1"). El 0 son los especiales, y esos se
 *   dejan fuera desde la pantalla.
 * @property nombre como la llama TMDB ("Temporada 1"), que no siempre coincide con [numero].
 * @property numeroDeEpisodios cuantos tiene. Cuenta los anunciados, no solo los emitidos.
 * @property estreno la fecha del primer episodio, o null si aun no se sabe.
 * @property poster el cartel propio de la temporada, que puede no existir.
 */
@Parcelize
data class TemporadaModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("season_number")
    val numero: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("episode_count")
    val numeroDeEpisodios: Int,

    @SerializedName("air_date")
    val estreno: String?,

    @SerializedName("poster_path")
    val poster: String?
) : Parcelable

/**
 * Un episodio dentro de una temporada.
 *
 * TMDB devuelve igual de completos los ya emitidos y los solo anunciados, asi que la fecha de
 * [emision] es lo unico que distingue unos de otros.
 *
 * @property numero el que hace dentro de su temporada, empezando por el 1.
 * @property sinopsis puede faltar, sobre todo en los que aun no se han emitido.
 * @property emision cuando se emitio o se emitira, o null si no hay fecha.
 * @property duracion en minutos, o null: TMDB no la tiene de muchos episodios.
 * @property imagen el fotograma del episodio ("still"), o null si no hay.
 */
@Parcelize
data class EpisodioModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("episode_number")
    val numero: Int,

    @SerializedName("name")
    val titulo: String,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("air_date")
    val emision: String?,

    @SerializedName("runtime")
    val duracion: Int?,

    @SerializedName("vote_average")
    val valoracion: Double,

    @SerializedName("still_path")
    val imagen: String?
) : Parcelable
