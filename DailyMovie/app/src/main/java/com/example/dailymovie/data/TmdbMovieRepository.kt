package com.example.dailymovie.data

import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.WebService
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ResultadoMulti
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call

/**
 * La implementacion de verdad: habla con TMDB.
 *
 * La clave de la API se queda aqui dentro. Antes la pasaba cada llamante, asi que cualquier
 * pantalla tenia que saber de ella; ahora los ViewModels solo piden peliculas.
 */
class TmdbMovieRepository(
    private val servicio: WebService = RetrofitClient.webService,
    private val apiKey: String = Constantes.API_KEY
) : MovieRepository {

    override fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.searchMovies(consulta, apiKey), alTerminar)

    override fun buscarTodo(consulta: String, alTerminar: (Resultado<List<Hallazgo>>) -> Unit) {
        servicio.buscarTodo(consulta, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(aHallazgos(it.resultados))) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit) {
        servicio.getTendencias(apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(aHallazgos(it.resultados))) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    /**
     * Pasa lo que devuelve TMDB a algo que la pantalla pueda pintar sin preguntarse que es.
     *
     * Se tiran los resultados sin titulo o sin tipo: TMDB cuela de vez en cuando entradas a
     * medias, y una tarjeta en blanco en mitad de la rejilla queda peor que no enseñarla.
     */
    private fun aHallazgos(crudos: List<ResultadoMulti>): List<Hallazgo> =
        crudos.mapNotNull { item ->
            val tipo = when (item.tipo) {
                "movie" -> TipoDeHallazgo.PELICULA
                "tv" -> TipoDeHallazgo.SERIE
                "person" -> TipoDeHallazgo.PERSONA
                else -> null
            } ?: return@mapNotNull null

            val titulo = (item.titulo ?: item.nombre)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            Hallazgo(
                id = item.id,
                titulo = titulo,
                subtitulo = when (tipo) {
                    TipoDeHallazgo.PELICULA -> item.estreno.orEmpty()
                    TipoDeHallazgo.SERIE -> item.primeraEmision.orEmpty()
                    // De la gente interesa a que se dedica, no una fecha
                    TipoDeHallazgo.PERSONA -> traducirOficio(item.oficio)
                },
                imagen = if (tipo == TipoDeHallazgo.PERSONA) item.foto else item.cartel,
                nota = item.nota ?: 0.0,
                tipo = tipo,
                relevancia = item.popularidad ?: 0.0
            )
        }

    /** TMDB devuelve el oficio siempre en ingles, aunque se le pida en español. */
    private fun traducirOficio(oficio: String?): String = when (oficio) {
        "Acting" -> "Interpretación"
        "Directing" -> "Dirección"
        "Writing" -> "Guion"
        "Production" -> "Producción"
        "Sound" -> "Sonido"
        "Camera" -> "Fotografía"
        "Art" -> "Arte"
        "Editing" -> "Montaje"
        "Costume & Make-Up" -> "Vestuario y maquillaje"
        "Visual Effects" -> "Efectos visuales"
        "Crew" -> "Equipo"
        else -> oficio.orEmpty()
    }

    override fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getNowPlayingMovies(apiKey), alTerminar)

    override fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getPopularMovies(apiKey), alTerminar)

    override fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getTopRatedMovies(apiKey), alTerminar)

    override fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getUpcomingMovies(apiKey), alTerminar)

    override fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getSimilarMovies(peliculaId, apiKey), alTerminar)

    override fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getRecommendedMovies(peliculaId, apiKey), alTerminar)

    override fun porGeneros(generos: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit) {
        // TMDB espera los generos separados por coma; con "|" seria "cualquiera de ellos"
        // y con "," "todos a la vez". Se usa la coma para afinar mas.
        listaDePeliculas(servicio.descubrirPeliculas(apiKey, generos.joinToString(",")), alTerminar)
    }

    override fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit) {
        servicio.getMovieDetails(peliculaId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) {
        servicio.getMovieProviders(peliculaId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) {
        servicio.getMovieCredits(peliculaId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) {
        // Se piden dos veces, en el idioma del dispositivo y en ingles, porque TMDB tiene
        // muchos mas trailers en ingles. Si la segunda falla nos quedamos con la primera.
        servicio.getMovieVideos(peliculaId, apiKey, LocaleUtil.getLanguageAndCountry()).enqueueSimple(
            onExito = { enIdiomaLocal ->
                val locales = trailersPrimero(enIdiomaLocal.results)
                servicio.getMovieVideos(peliculaId, apiKey, "en-US").enqueueSimple(
                    onExito = { alTerminar(Resultado.Exito(locales + trailersPrimero(it.results))) },
                    onError = { alTerminar(Resultado.Exito(locales)) }
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) {
        servicio.getGeneros(apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.generos)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    /** Los trailers van delante; el resto (clips, featurettes) detras. */
    private fun trailersPrimero(videos: List<VideoModel>): List<VideoModel> =
        videos.filter { it.type == "Trailer" } + videos.filter { it.type != "Trailer" }

    private fun listaDePeliculas(
        llamada: Call<com.example.dailymovie.client.response.MoviesResponse>,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        llamada.enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.results)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }
}
