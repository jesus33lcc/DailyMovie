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
 *
 * @property fecha el estreno de la pelicula o la primera emision de la serie, segun lo que
 *   sea. Se guarda entera aunque en la tarjeta solo se enseñe el año.
 * @property nota de 0 a 10, tal y como estaba cuando se guardo.
 * @property cartel ruta del cartel, o null si no habia.
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

    /**
     * Lo devuelve a pelicula, que es lo que esperan las pantallas de ficha.
     *
     * Nadie comprueba nada aqui: llamarlo sobre algo con [esSerie] a true sale bien y da un
     * objeto que no significa nada, asi que hay que mirar el tipo antes.
     */
    fun aPelicula() = MovieModel(id, titulo, fecha, nota, cartel)

    /** Lo mismo pero a serie, con la misma advertencia al reves. */
    fun aSerie() = SerieModel(id, titulo, fecha, nota, cartel)

    companion object {
        /**
         * @param pelicula la que se va a guardar en una lista.
         * @return lo mismo pero en el formato comun, ya marcado como pelicula.
         */
        fun de(pelicula: MovieModel) = Guardado(
            id = pelicula.id,
            titulo = pelicula.title,
            fecha = pelicula.releaseDate,
            nota = pelicula.voteAverage,
            cartel = pelicula.posterPath,
            esSerie = false
        )

        /**
         * @param serie la que se va a guardar en una lista. Su estreno puede ser null y aqui
         *   se queda como cadena vacia, que es lo que sabe manejar la tarjeta.
         * @return lo mismo pero en el formato comun, ya marcado como serie.
         */
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
