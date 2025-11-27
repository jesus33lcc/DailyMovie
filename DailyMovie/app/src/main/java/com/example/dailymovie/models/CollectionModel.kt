package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
/**
 * La saga a la que pertenece una pelicula ("El Padrino - Coleccion").
 *
 * Llega dentro de los detalles de la pelicula y solo si es de alguna. Con su [id] se pueden
 * pedir despues todas las de la saga, que es como se llega a verlas en orden.
 *
 * @property posterPath el cartel de la saga entera, que no es el de ninguna de sus peliculas.
 *   Puede ser null.
 * @property backdropPath la imagen apaisada, que tambien puede faltar.
 */
@Parcelize
data class CollectionModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?
) : Parcelable
