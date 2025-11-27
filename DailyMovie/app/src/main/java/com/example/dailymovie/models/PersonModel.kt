package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Ficha de una persona: vale igual para un actor que para un director.
 *
 * @property biografia puede llegar vacia o a null aunque sea alguien conocido: TMDB solo la
 *   tiene traducida en los nombres mas famosos y en el resto la deja en blanco.
 * @property nacimiento la fecha como "1937-04-25", o null si no consta.
 * @property fallecimiento null si sigue vivo. Es la unica forma de saberlo, no hay ninguna
 *   marca aparte.
 * @property lugarDeNacimiento ciudad, region y pais en un solo texto, tal cual lo escribe
 *   TMDB.
 * @property foto ruta de la foto, o null si no tiene.
 */
@Parcelize
data class PersonModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("biography")
    val biografia: String?,

    @SerializedName("birthday")
    val nacimiento: String?,

    @SerializedName("deathday")
    val fallecimiento: String?,

    @SerializedName("place_of_birth")
    val lugarDeNacimiento: String?,

    @SerializedName("profile_path")
    val foto: String?,

    /** TMDB lo llama "known_for_department": Acting, Directing... */
    @SerializedName("known_for_department")
    val oficio: String?
) : Parcelable
