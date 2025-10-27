package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta person/popular.
 *
 * De cada persona solo interesa el nombre, la foto y a que se dedica: es para enseñar caras
 * en el onboarding, no para pintar una ficha.
 */
data class PersonasPopularesResponse(
    @SerializedName("results")
    val resultados: List<PersonaPopular> = emptyList()
)

data class PersonaPopular(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("profile_path")
    val foto: String?,

    @SerializedName("known_for_department")
    val oficio: String?
)
