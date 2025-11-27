package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

/**
 * Un video de los que TMDB tiene de una pelicula o una serie: trailer, teaser, clip...
 *
 * @property key el id del video en el sitio donde esta colgado, o sea en YouTube. Es lo unico
 *   que hace falta para sacar la miniatura y para abrirlo.
 * @property id el id del video dentro de TMDB. No sirve para abrirlo; para eso esta [key].
 * @property site donde esta colgado. En la practica siempre "YouTube".
 * @property type que clase de video es ("Trailer", "Teaser", "Clip"). En ingles, y es lo que
 *   se mira para poner los trailers los primeros de la lista.
 * @property iso6391 el idioma del video en dos letras, e [iso31661] el pais. La app no los
 *   mira: para tener trailers en español y en ingles se pide la lista dos veces.
 * @property size la resolucion (1080, 720...). No se usa.
 */
data class VideoModel(
    @SerializedName("id")
    val id: String,

    @SerializedName("iso_639_1")
    val iso6391: String,

    @SerializedName("iso_3166_1")
    val iso31661: String,

    @SerializedName("key")
    val key: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("site")
    val site: String,

    @SerializedName("size")
    val size: Int,

    @SerializedName("type")
    val type: String
)
