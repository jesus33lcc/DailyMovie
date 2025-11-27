package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
/**
 * Un genero de cine de TMDB ("Drama", "Ciencia ficcion").
 *
 * El [id] es lo que se le manda a discover para filtrar por el; el [name] llega ya traducido
 * al idioma del aparato. En series la cosa no es tan facil, y por eso existe
 * [GeneroExplorable].
 */
@Parcelize
data class GenreModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
) : Parcelable
