package com.example.dailymovie.client.response

import com.example.dailymovie.models.ProviderResultModel
import com.google.gson.annotations.SerializedName

/**
 * Donde se puede ver una pelicula o una serie, pais por pais.
 *
 * TMDB los manda todos de golpe, con el codigo de pais como clave del mapa ("ES", "US"...), asi
 * que hay que sacar el del aparato con `LocaleUtil.getDeviceCountry()`. Si ese pais no esta en
 * el mapa es que ahi no se puede ver en ningun sitio, no que haya fallado la peticion.
 */
data class ProviderResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: Map<String, ProviderResultModel>
)
