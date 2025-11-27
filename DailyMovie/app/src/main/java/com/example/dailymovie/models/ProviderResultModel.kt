package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

/**
 * Donde se puede ver una pelicula o una serie, en un pais concreto.
 *
 * TMDB devuelve las plataformas agrupadas por pais, asi que esto es lo que hay dentro de la
 * clave del pais del aparato. Si ese pais no viene en la respuesta es que ahi no consta en
 * ningun sitio, y entonces la seccion no se enseña.
 *
 * Las tres listas pueden venir a null, y null no es lo mismo que vacia: significa que de esa
 * forma no esta disponible.
 *
 * @property link la pagina de TMDB con todas las formas de verla.
 * @property flatrate incluido en la suscripcion. Es lo unico que enseña la app: pagar aparte
 *   por verla es otra cosa y llenaria la ficha de logos repetidos.
 * @property rent en alquiler suelto.
 * @property buy en compra.
 */
data class ProviderResultModel(
    @SerializedName("link")
    val link: String,
    @SerializedName("flatrate")
    val flatrate: List<ProviderDetailModel>?,
    @SerializedName("rent")
    val rent: List<ProviderDetailModel>?,
    @SerializedName("buy")
    val buy: List<ProviderDetailModel>?
)
