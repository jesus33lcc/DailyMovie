package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
/**
 * Una pelicula en su version corta: lo justo para pintar una tarjeta.
 *
 * Es lo que devuelven todas las listas de TMDB (cartelera, populares, discover, busqueda) y
 * ademas es lo que se guarda tal cual en Firestore. Ahi esta la trampa: como se manda el
 * objeto entero, en la base quedan escritos estos nombres de Kotlin (`releaseDate`,
 * `voteAverage`, `posterPath`) y no el snake_case de TMDB. Renombrar un campo de aqui deja
 * ilegible todo lo que ya haya guardado la gente.
 *
 * @property releaseDate el estreno como "1972-03-14". Puede llegar vacia en lo que aun no
 *   tiene fecha puesta.
 * @property voteAverage la nota de 0 a 10. Un 0 casi siempre quiere decir que todavia no la
 *   ha votado nadie, no que sea mala.
 * @property posterPath ruta del cartel, o null si TMDB no tiene ninguno.
 */
@Parcelize
data class MovieModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("release_date")
    val releaseDate: String,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("poster_path")
    val posterPath: String?
) : Parcelable