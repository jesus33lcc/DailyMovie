package com.example.dailymovie.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Lo que puede salir en una busqueda. */
enum class TipoDeHallazgo { PELICULA, SERIE, PERSONA }

/**
 * Un resultado de busqueda, sea lo que sea.
 *
 * El buscador usa search/multi de TMDB, que devuelve peliculas, series y gente mezcladas en
 * la misma lista y ordenadas por lo que mas encaja con lo que has escrito. Cada tipo trae sus
 * propios nombres de campo (title/name, release_date/first_air_date, poster_path/profile_path),
 * asi que se normaliza aqui y la pantalla ya solo pinta titulo, subtitulo e imagen.
 */
@Parcelize
data class Hallazgo(
    val id: Int,
    val titulo: String,
    /** El año en peliculas y series; el oficio en las personas. */
    val subtitulo: String,
    val imagen: String?,
    val nota: Double,
    val tipo: TipoDeHallazgo,
    /** Para ordenar por lo que mas encaja, que es como lo manda TMDB. */
    val relevancia: Double = 0.0
) : Parcelable {

    /** El año, para poder ordenar por fecha sin arrastrar la fecha entera. */
    val ano: Int get() = subtitulo.take(4).toIntOrNull() ?: 0

    companion object {
        /**
         * Las tarjetas de la portada y de series son las mismas que las del buscador, asi que
         * en vez de tener tres adaptadores calcados se convierte a esto y se usa el mismo.
         */
        fun de(pelicula: MovieModel) = Hallazgo(
            id = pelicula.id,
            titulo = pelicula.title,
            subtitulo = pelicula.releaseDate,
            imagen = pelicula.posterPath,
            nota = pelicula.voteAverage,
            tipo = TipoDeHallazgo.PELICULA
        )

        fun de(serie: SerieModel) = Hallazgo(
            id = serie.id,
            titulo = serie.titulo,
            subtitulo = serie.estreno.orEmpty(),
            imagen = serie.poster,
            nota = serie.valoracion,
            tipo = TipoDeHallazgo.SERIE
        )
    }
}
