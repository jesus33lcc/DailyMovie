package com.example.dailymovie.client.response

import com.example.dailymovie.models.CastMemberModel
import com.example.dailymovie.models.CrewMemberModel
import com.google.gson.annotations.SerializedName

/**
 * Quien ha hecho una pelicula o una serie.
 *
 * TMDB lo parte en dos y aqui se deja igual: en [cast] esta la gente que sale en pantalla, ya
 * ordenada por importancia del papel, y en [crew] la que trabaja detras. Al director hay que
 * buscarlo en [crew] mirando su `job`, no viene señalado de ninguna otra forma.
 */
data class CreditResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cast")
    val cast: List<CastMemberModel>,
    @SerializedName("crew")
    val crew: List<CrewMemberModel>
)
