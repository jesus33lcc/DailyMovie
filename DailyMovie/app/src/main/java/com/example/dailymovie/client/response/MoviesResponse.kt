package com.example.dailymovie.client.response

import com.example.dailymovie.models.MovieModel
import com.google.gson.annotations.SerializedName

/**
 * El sobre en el que TMDB mete cualquier lista de peliculas.
 *
 * Lo devuelven casi todos los endpoints de cine (cartelera, populares, similares, discover...),
 * y por eso el repositorio tiene un solo sitio que lo abre y saca [results].
 *
 * De la pagina no se guarda nada aqui: para las listas que se van cargando segun se baja se usa
 * `BusquedaMultiResponse`, que si trae el numero de pagina y el total.
 */
data class MoviesResponse(
    @SerializedName("results")
    var results: List<MovieModel>
)
