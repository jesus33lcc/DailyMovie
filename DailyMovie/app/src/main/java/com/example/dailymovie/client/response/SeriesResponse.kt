package com.example.dailymovie.client.response

import com.example.dailymovie.models.EpisodioModel
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.TemporadaModel
import com.google.gson.annotations.SerializedName

/** Listas de series: populares, mejor valoradas, en emision y busqueda. */
data class SeriesResponse(
    @SerializedName("results")
    val results: List<SerieModel>
)

/** Ficha completa de una serie, con sus temporadas. */
data class SerieDetailsResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val titulo: String,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("first_air_date")
    val estreno: String?,

    @SerializedName("last_air_date")
    val ultimaEmision: String?,

    @SerializedName("vote_average")
    val valoracion: Double,

    @SerializedName("poster_path")
    val poster: String?,

    @SerializedName("number_of_seasons")
    val numeroDeTemporadas: Int,

    @SerializedName("number_of_episodes")
    val numeroDeEpisodios: Int,

    @SerializedName("status")
    val estado: String?,

    @SerializedName("genres")
    val generos: List<GenreModel>,

    @SerializedName("seasons")
    val temporadas: List<TemporadaModel>
)

/** Una temporada con sus episodios dentro. */
data class SeasonResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("season_number")
    val numero: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("episodes")
    val episodios: List<EpisodioModel>
)
