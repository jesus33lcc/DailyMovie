package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

/**
 * Alguien del equipo: direccion, guion, fotografia, musica...
 *
 * Al director hay que buscarlo aqui comparando [job] con "Director", porque TMDB no lo señala
 * de ninguna otra forma: viene mezclado con el resto del equipo, que pueden ser cien nombres.
 *
 * @property department el area en la que trabaja ("Directing", "Writing"). En ingles.
 * @property job el puesto exacto dentro de esa area ("Director", "Screenplay"). En ingles
 *   tambien, por eso se compara contra el texto en ingles y no contra uno traducido.
 * @property knownForDepartment por lo que se le conoce en general, que no tiene por que ser
 *   lo que hizo en esta pelicula.
 * @property profilePath ruta de la foto, o null si no tiene.
 * @property creditId identifica el credito, no a la persona. Para su ficha hace falta [id].
 * @property gender codigo numerico de TMDB, no un texto. La app no lo usa.
 */
data class CrewMemberModel(
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
    @SerializedName("credit_id")
    val creditId: String,
    @SerializedName("department")
    val department: String,
    @SerializedName("job")
    val job: String
)
