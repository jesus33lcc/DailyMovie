package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * La ficha completa de una pelicula: todo lo que se enseña en su pantalla de detalle.
 *
 * Es mucho mas de lo que trae [MovieModel], por eso se pide aparte al abrir la ficha y no al
 * pintar las listas.
 *
 * Varios numeros usan el 0 como "no se sabe", que no es lo mismo que cero de verdad. Por eso
 * la ficha esconde esas filas en vez de enseñar un "0 $" que seria mentira.
 *
 * @property belongsToCollection la saga, si pertenece a alguna. Null en la mayoria.
 * @property budget presupuesto en dolares, y 0 si no consta.
 * @property revenue lo que recaudo, en dolares, con el mismo 0 de "no consta".
 * @property runtime duracion en minutos, y otro 0 que quiere decir que TMDB no lo sabe.
 * @property imdbId el id en IMDb ("tt0068646"), que no tiene nada que ver con [id]. Hace
 *   falta para abrir la ficha alli, y puede faltar.
 * @property tagline la frase del cartel. Muchas peliculas no la tienen traducida y llega
 *   vacia o a null.
 * @property status en que punto esta ("Released", "Post Production"). En ingles.
 * @property originalTitle el titulo en su idioma original, que a veces no se parece nada al
 *   traducido de [title].
 * @property originCountry los paises de origen en dos letras ("US", "IT").
 * @property video bandera de TMDB que la app no mira. Los trailers se piden por su lado.
 * @property voteCount cuanta gente ha votado. Sirve para saber si fiarse de [voteAverage].
 */
@Parcelize
data class MovieDetailsModel(
    @SerializedName("adult")
    val adult: Boolean,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("belongs_to_collection")
    val belongsToCollection: CollectionModel?,

    @SerializedName("budget")
    val budget: Int,

    @SerializedName("genres")
    val genres: List<GenreModel>,

    @SerializedName("homepage")
    val homepage: String?,

    @SerializedName("id")
    val id: Int,

    @SerializedName("imdb_id")
    val imdbId: String?,

    @SerializedName("origin_country")
    val originCountry: List<String>,

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("overview")
    val overview: String,

    @SerializedName("popularity")
    val popularity: Double,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("production_companies")
    val productionCompanies: List<ProductionCompanyModel>,

    @SerializedName("release_date")
    val releaseDate: String,

    @SerializedName("revenue")
    val revenue: Long,

    @SerializedName("runtime")
    val runtime: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("tagline")
    val tagline: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("video")
    val video: Boolean,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int
) : Parcelable