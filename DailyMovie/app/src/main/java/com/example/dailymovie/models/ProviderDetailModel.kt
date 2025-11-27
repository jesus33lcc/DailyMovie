package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

/**
 * Una plataforma donde se puede ver algo: Netflix, Movistar Plus+, Filmin...
 *
 * @property providerName el nombre que se enseña debajo del logo.
 * @property logoPath ruta del logo, que es lo que de verdad se reconoce de un vistazo.
 * @property providerId el id de TMDB de la plataforma, el que se usa para filtrar en el
 *   buscador. Ojo: no es el mismo en todos los paises.
 * @property displayPriority el orden en el que TMDB sugiere enseñarlas, de menor a mayor.
 *   La app no lo usa: las pinta como vienen.
 */
data class ProviderDetailModel(
    @SerializedName("logo_path")
    val logoPath: String,
    @SerializedName("provider_id")
    val providerId: Int,
    @SerializedName("provider_name")
    val providerName: String,
    @SerializedName("display_priority")
    val displayPriority: Int
)
