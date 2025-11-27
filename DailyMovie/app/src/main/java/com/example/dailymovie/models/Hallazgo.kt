package com.example.dailymovie.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Lo que puede salir en una busqueda. */
enum class TipoDeHallazgo { PELICULA, SERIE, PERSONA, SAGA }

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
    /** El cartel en peliculas, series y sagas; la foto en las personas. Null si no hay. */
    val imagen: String?,
    /** De 0 a 10. Se queda en 0 en las personas y las sagas, que no tienen nota. */
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
         *
         * @param pelicula la que se va a pintar.
         * @return el hallazgo con el año de subtitulo y sin relevancia, porque aqui el orden
         *   lo pone quien manda la lista.
         */
        fun de(pelicula: MovieModel) = Hallazgo(
            id = pelicula.id,
            titulo = pelicula.title,
            subtitulo = pelicula.releaseDate,
            imagen = pelicula.posterPath,
            nota = pelicula.voteAverage,
            tipo = TipoDeHallazgo.PELICULA
        )

        /**
         * Algo de la filmografia de alguien, sea pelicula o serie.
         *
         * Desde que se pide combined_credits en vez de movie_credits, en la ficha de una
         * persona salen las dos cosas, y el tipo decide que ficha se abre al tocarla.
         *
         * @param trabajo una entrada de su filmografia.
         */
        fun de(trabajo: com.example.dailymovie.client.response.PeliculaDePersona) = Hallazgo(
            id = trabajo.id,
            titulo = trabajo.comoSeLlama,
            subtitulo = trabajo.cuandoSalio,
            imagen = trabajo.poster,
            nota = trabajo.valoracion,
            tipo = if (trabajo.esSerie) TipoDeHallazgo.SERIE else TipoDeHallazgo.PELICULA
        )

        /**
         * Una saga entera, para poder abrirla y ver sus peliculas en orden.
         *
         * @param saga la coleccion que ha encontrado la busqueda.
         * @return un hallazgo sin año ni nota, porque una saga no tiene ninguna de las dos
         *   cosas, y con la relevancia al maximo para que salga la primera.
         */
        fun de(saga: com.example.dailymovie.client.response.SagaResponse) = Hallazgo(
            id = saga.id,
            titulo = saga.nombre,
            subtitulo = "",
            imagen = saga.cartel,
            nota = 0.0,
            tipo = TipoDeHallazgo.SAGA,
            // Por encima de todo lo demas: si buscas "El Padrino" lo primero que quieres ver
            // es la trilogia, no la tercera parte suelta.
            relevancia = Double.MAX_VALUE
        )

        /**
         * @param serie la que se va a pintar. Si no tiene fecha de estreno el subtitulo se
         *   queda vacio, que es mejor que enseñar un año inventado.
         */
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
