package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/** Ficha de una persona: vale igual para un actor que para un director. */
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
