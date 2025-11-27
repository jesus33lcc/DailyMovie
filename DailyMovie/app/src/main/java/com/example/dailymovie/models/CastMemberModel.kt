package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

/**
 * Alguien que sale en pantalla, tal y como viene en el reparto de TMDB.
 *
 * De todo esto la app solo enseña la foto, el nombre y el personaje, y usa el [id] para abrir
 * su ficha. El resto se deja porque llega en la misma respuesta y quitarlo no ahorra nada.
 *
 * @property character el personaje que interpreta, que es lo que va debajo del nombre.
 * @property profilePath ruta de la foto, o null si no tiene: entonces se pinta la silueta.
 * @property knownForDepartment por lo que se le conoce en general ("Acting", "Directing").
 *   Llega siempre en ingles, TMDB no lo traduce.
 * @property order el sitio que ocupa en los creditos: el 0 es el papel principal. TMDB ya
 *   manda el reparto ordenado por esto, asi que no hace falta ordenarlo aqui.
 * @property castId id de esa aparicion concreta, no de la persona. Para abrir su ficha hace
 *   falta [id].
 * @property creditId lo mismo: identifica el credito, no a quien lo firma.
 * @property gender codigo numerico de TMDB, no un texto. La app no lo usa.
 */
data class CastMemberModel(
    @SerializedName("adult")
    val adult: Boolean,
    @SerializedName("gender")
    val gender: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("known_for_department")
    val knownForDepartment: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("original_name")
    val originalName: String,
    @SerializedName("popularity")
    val popularity: Double,
    @SerializedName("profile_path")
    val profilePath: String?,
    @SerializedName("cast_id")
    val castId: Int,
    @SerializedName("character")
    val character: String,
    @SerializedName("credit_id")
    val creditId: String,
    @SerializedName("order")
    val order: Int
)
