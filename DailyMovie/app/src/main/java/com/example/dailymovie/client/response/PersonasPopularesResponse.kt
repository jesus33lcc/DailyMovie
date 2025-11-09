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

/**
 * Una cara para la rejilla del onboarding.
 *
 * La foto puede faltar, y ahi no vale con enseñar un hueco: en una pantalla que va de elegir
 * caras, una tarjeta sin foto no se puede ni reconocer. El repositorio se las quita antes.
 *
 * El oficio llega siempre en ingles ("Acting", "Directing") aunque se pida en español.
 */
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
