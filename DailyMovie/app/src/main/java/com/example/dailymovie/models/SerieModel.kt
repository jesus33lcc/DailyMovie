package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Serie en su version corta, la que se enseña en las listas.
 *
 * TMDB usa nombres distintos para series y peliculas aunque el dato sea el mismo: aqui
 * "name" y "first_air_date" son el titulo y la fecha de estreno.
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

/** Una temporada, tal y como aparece en la ficha de la serie. */
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

/** Un episodio dentro de una temporada. */
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
