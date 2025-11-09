package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta search/collection.
 *
 * Una coleccion en TMDB es lo que aqui se llama saga: "El Padrino - Coleccion", "El Señor de
 * los Anillos - Coleccion". Buscando por titulo normal no salen, hay un endpoint aparte.
 */
data class BusquedaDeSagasResponse(
    @SerializedName("results")
    val resultados: List<SagaResponse> = emptyList()
)

/**
 * La cabecera de una saga en los resultados de busqueda.
 *
 * Aqui todavia no vienen las peliculas de dentro: para eso hay que pedir el detalle con el
 * [id], que es un id de coleccion y no vale como id de pelicula.
 */
data class SagaResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("poster_path")
    val cartel: String?,

    @SerializedName("overview")
    val sinopsis: String?
)

/**
 * El detalle de una saga: sus peliculas en orden.
 *
 * TMDB las devuelve por orden de estreno, que es justo el que interesa para verlas.
 */
data class DetalleDeSagaResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val nombre: String,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("parts")
    val peliculas: List<PeliculaDeSaga> = emptyList()
)

/**
 * Una de las peliculas que forman la saga.
 *
 * Trae menos campos que la ficha completa, los justos para pintar el cartel de la lista. Si el
 * usuario toca una, se pide su ficha aparte con el mismo [id], que aqui si es de pelicula.
 */
data class PeliculaDeSaga(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val titulo: String?,

    @SerializedName("release_date")
    val estreno: String?,

    @SerializedName("poster_path")
    val cartel: String?,

    @SerializedName("backdrop_path")
    val fondo: String?,

    @SerializedName("overview")
    val sinopsis: String?,

    @SerializedName("vote_average")
    val nota: Double = 0.0
)
