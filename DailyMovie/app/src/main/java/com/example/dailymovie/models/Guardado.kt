package com.example.dailymovie.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Algo guardado en una lista, sea pelicula o serie.
 *
 * Una lista puede tener las dos cosas, pero en Firestore van en campos distintos porque el
 * dato no es igual (una serie tiene primera emision, no estreno). Para pintarlas en la misma
 * cuadricula hace falta algo que valga para ambas, y resulta que lo que se enseña es lo
 * mismo: cartel, titulo, año y nota.
 *
 * `esSerie` es lo unico que hay que arrastrar, y sirve para dos cosas: saber a que ficha ir
 * al tocarla y a que campo de Firestore volver si se quita.
 */
@Parcelize
data class Guardado(
    val id: Int,
    val titulo: String,
    val fecha: String,
    val nota: Double,
    val cartel: String?,
    val esSerie: Boolean
) : Parcelable {

    fun aPelicula() = MovieModel(id, titulo, fecha, nota, cartel)

    fun aSerie() = SerieModel(id, titulo, fecha, nota, cartel)

    companion object {
        fun de(pelicula: MovieModel) = Guardado(
            id = pelicula.id,
            titulo = pelicula.title,
            fecha = pelicula.releaseDate,
            nota = pelicula.voteAverage,
            cartel = pelicula.posterPath,
            esSerie = false
        )

        fun de(serie: SerieModel) = Guardado(
            id = serie.id,
            titulo = serie.titulo,
            fecha = serie.estreno.orEmpty(),
            nota = serie.valoracion,
            cartel = serie.poster,
            esSerie = true
        )
    }
}
