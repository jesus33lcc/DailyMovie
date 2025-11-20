package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta tv/{id}/content_ratings.
 *
 * Cada pais clasifica por su cuenta, asi que la misma serie puede ser "16" aqui y "TV-MA" en
 * Estados Unidos. Solo interesa la del pais del usuario.
 */
data class ClasificacionesSerieResponse(
    @SerializedName("results")
    val resultados: List<ClasificacionDePais> = emptyList()
)

data class ClasificacionDePais(
    @SerializedName("iso_3166_1")
    val pais: String,

    /** Puede llegar en blanco aunque el pais este en la lista. */
    @SerializedName("rating")
    val clasificacion: String?
)
