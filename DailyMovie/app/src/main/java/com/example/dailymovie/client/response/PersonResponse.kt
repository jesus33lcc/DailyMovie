package com.example.dailymovie.client.response

import com.example.dailymovie.models.GenreModel
import com.google.gson.annotations.SerializedName

/** La ficha de la persona llega con la misma forma que PersonModel. */
typealias PersonResponse = com.example.dailymovie.models.PersonModel

/**
 * Filmografia de una persona.
 *
 * TMDB la parte en dos: "cast" es en lo que ha actuado y "crew" en lo que ha trabajado
 * detras de la camara. Un director aparece en "crew" con job = "Director".
 */
data class PersonCreditsResponse(
    @SerializedName("cast")
    val actuaciones: List<PeliculaDePersona> = emptyList(),

    @SerializedName("crew")
    val trabajos: List<PeliculaDePersona> = emptyList()
)

/**
 * Un trabajo de la filmografia: puede ser una pelicula o una serie.
 *
 * Es el mismo tipo para las dos cosas porque combined_credits las devuelve mezcladas, y eso
 * obliga a tener los campos duplicados: el titulo llega en "title" si es cine y en "name" si es
 * television, y la fecha igual. Para no repetir esa comprobacion en cada pantalla estan
 * [comoSeLlama], [cuandoSalio] y [esSerie].
 *
 * TMDB devuelve aqui muchisimo relleno (promocionales, apariciones de un minuto, making-of),
 * asi que el repositorio filtra y ordena antes de que esto llegue a la ficha.
 */
data class PeliculaDePersona(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val titulo: String?,

    @SerializedName("release_date")
    val estreno: String?,

    @SerializedName("vote_average")
    val valoracion: Double,

    @SerializedName("poster_path")
    val poster: String?,

    /** El personaje que interpreta, si viene de "cast". */
    @SerializedName("character")
    val personaje: String?,

    /** El puesto que ocupa, si viene de "crew": Director, Writer... */
    @SerializedName("job")
    val puesto: String?,

    /**
     * El titulo cuando es una serie: combined_credits usa "name" en vez de "title".
     *
     * Se guarda aparte y no se mezcla con [titulo] porque hace falta saber cual de los dos
     * ha venido para decidir si al tocarla se abre la ficha de pelicula o la de serie.
     */
    @SerializedName("name")
    val nombreDeSerie: String? = null,

    @SerializedName("first_air_date")
    val primeraEmision: String? = null,

    @SerializedName("media_type")
    val tipo: String? = null,

    /**
     * Lo conocido que es este trabajo.
     *
     * Es con lo que se ordena la filmografia. Ordenar por nota no vale: los making-of y los
     * bloopers tienen un 10 con dos votos y se comian el primer puesto de la ficha, asi que
     * un actor de series salia encabezado por un documental de rodaje.
     */
    @SerializedName("popularity")
    val popularidad: Double = 0.0,

    @SerializedName("vote_count")
    val votos: Int = 0,

    /**
     * Que puesto ocupa en el reparto: 0 es el protagonista.
     *
     * Sirve para separar los papeles de verdad de los cameos. Sin esto, la ficha de Pedro
     * Pascal empezaba por "Ley y orden", donde sale un capitulo, solo porque la serie tiene
     * treinta temporadas y mucha popularidad.
     */
    @SerializedName("order")
    val puestoEnElReparto: Int? = null,

    /**
     * En cuantos episodios sale, cuando es una serie.
     *
     * En television el puesto del reparto no significa gran cosa: en "Ley y orden" Pedro
     * Pascal sale en un episodio y aun asi aparece bien colocado en la ficha de ese capitulo.
     * Lo que separa un papel fijo de una aparicion suelta es cuantos episodios hace.
     */
    @SerializedName("episode_count")
    val episodios: Int? = null
) {
    /** Si esto es una serie y no una pelicula. */
    val esSerie: Boolean get() = tipo == "tv"

    /** El titulo, venga de donde venga. */
    val comoSeLlama: String get() = (titulo ?: nombreDeSerie).orEmpty()

    /** La fecha, venga de donde venga. */
    val cuandoSalio: String get() = (estreno ?: primeraEmision).orEmpty()
}

/** Lista de generos de TMDB, para el onboarding y la recomendacion. */
data class GenresResponse(
    @SerializedName("genres")
    val generos: List<GenreModel> = emptyList()
)
