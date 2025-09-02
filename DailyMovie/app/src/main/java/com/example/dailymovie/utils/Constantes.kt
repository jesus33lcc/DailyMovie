package com.example.dailymovie.utils

import com.example.dailymovie.BuildConfig

object Constantes {
    // La clave llega desde local.properties al compilar (ver app/build.gradle.kts), asi no
    // se queda escrita en el codigo. Se mantiene aqui para no cambiar quien la usa.
    val API_KEY: String = BuildConfig.TMDB_API_KEY
    const val BASE_URL = "https://api.themoviedb.org/3/";
    const val IMAGE_URL = "https://image.tmdb.org/t/p/w500/"
    const val YOUTUBE_URL = "https://www.youtube.com/watch?v="
    const val BASE_MOVIE_URL = "https://www.themoviedb.org/movie/"
}