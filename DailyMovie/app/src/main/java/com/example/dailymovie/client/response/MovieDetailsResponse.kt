package com.example.dailymovie.client.response

import com.example.dailymovie.models.CollectionModel
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.ProductionCompanyModel
import com.google.gson.annotations.SerializedName

/**
 * La ficha completa de una pelicula, tal cual la manda TMDB.
 *
 * Los nombres estan en ingles porque es codigo heredado y se ha respetado el idioma que ya
 * tenia; lo que se añadio despues (los tres extras del final) si va en español.
 *
 * Ojo con los tres ultimos campos: son opcionales porque solo llegan si la peticion pidio
 * `append_to_response`. Si alguien llama al endpoint sin ese parametro, la galeria, la
 * clasificacion por edad y el id de IMDb vienen a null y las secciones de la ficha se quedan
 * vacias sin que falle nada.
 *
 * `budget` y `revenue` valen 0 cuando TMDB no sabe la cifra, que no es lo mismo que gratis.
 */
data class MovieDetailsResponse(
    @SerializedName("adult")
    val adult: Boolean,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("belongs_to_collection")
    val belongsToCollection: CollectionModel?,

    @SerializedName("budget")
    val budget: Int,

    @SerializedName("genres")
    val genres: List<GenreModel> = emptyList(),

    @SerializedName("homepage")
    val homepage: String?,

    @SerializedName("id")
    val id: Int,

    @SerializedName("imdb_id")
    val imdbId: String?,

    @SerializedName("origin_country")
    val originCountry: List<String> = emptyList(),

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("overview")
    val overview: String,

    @SerializedName("popularity")
    val popularity: Double,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("production_companies")
    val productionCompanies: List<ProductionCompanyModel> = emptyList(),

    @SerializedName("release_date")
    val releaseDate: String,

    @SerializedName("revenue")
    val revenue: Long,

    @SerializedName("runtime")
    val runtime: Int,

    @SerializedName("status")
    val status: String,

    @SerializedName("tagline")
    val tagline: String?,

    @SerializedName("title")
    val title: String,

    @SerializedName("video")
    val video: Boolean,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int,

    // ---- Extras que llegan con append_to_response ----

    @SerializedName("images")
    val imagenes: ImagenesResponse?,

    @SerializedName("release_dates")
    val fechasDeEstreno: FechasDeEstrenoResponse?,

    @SerializedName("external_ids")
    val idsExternos: IdsExternosResponse?
)

/** Fondos, carteles y logos que TMDB tiene de una pelicula. */
data class ImagenesResponse(
    @SerializedName("backdrops")
    val fondos: List<ImagenTmdb> = emptyList(),

    @SerializedName("posters")
    val carteles: List<ImagenTmdb> = emptyList(),

    @SerializedName("logos")
    val logos: List<ImagenTmdb> = emptyList()
)

/**
 * Una imagen suelta de la galeria.
 *
 * [ruta] es un trozo de camino, no una URL: hay que pegarle delante la base de imagenes de
 * TMDB y el tamaño que se quiera. El ancho y el alto vienen para poder elegir las buenas sin
 * tener que descargarlas antes.
 */
data class ImagenTmdb(
    @SerializedName("file_path")
    val ruta: String,

    @SerializedName("width")
    val ancho: Int,

    @SerializedName("height")
    val alto: Int,

    @SerializedName("vote_average")
    val valoracion: Double
)

/**
 * La clasificacion por edad, que cambia en cada pais.
 * En España son 'A', 'A/i', 'A/fi', '7', '12', '16', '18' y 'X'.
 */
data class FechasDeEstrenoResponse(
    @SerializedName("results")
    val porPais: List<EstrenoEnPais> = emptyList()
)

/**
 * Los estrenos de una pelicula en un pais concreto.
 *
 * Hay varios por pais porque TMDB cuenta por separado el cine, el digital y el fisico, y no
 * todos traen clasificacion.
 */
data class EstrenoEnPais(
    @SerializedName("iso_3166_1")
    val pais: String,

    @SerializedName("release_dates")
    val estrenos: List<EstrenoConClasificacion> = emptyList()
)

/**
 * Un estreno con su clasificacion por edad.
 *
 * La clasificacion llega muchas veces como cadena vacia en vez de faltar, asi que no basta con
 * comprobar que no es null para darla por buena.
 */
data class EstrenoConClasificacion(
    @SerializedName("certification")
    val clasificacion: String?,

    @SerializedName("release_date")
    val fecha: String?
)

/** Ids en otras webs. De momento se usa el de IMDb. */
data class IdsExternosResponse(
    @SerializedName("imdb_id")
    val imdb: String?
)