package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Las plataformas de streaming que operan en un pais.
 *
 * Sirve para poder filtrar por "lo que puedo ver en Netflix": TMDB necesita el id de la
 * plataforma, y ese id no es el mismo en todas partes.
 */
data class PlataformasResponse(
    @SerializedName("results")
    val plataformas: List<PlataformaDisponible> = emptyList()
)

data class PlataformaDisponible(
    @SerializedName("provider_id")
    val id: Int,

    @SerializedName("provider_name")
    val nombre: String,

    @SerializedName("logo_path")
    val logo: String?,

    /** Lo que TMDB considera mas usadas en ese pais; sirve para ordenarlas. */
    @SerializedName("display_priority")
    val prioridad: Int = 999
)
