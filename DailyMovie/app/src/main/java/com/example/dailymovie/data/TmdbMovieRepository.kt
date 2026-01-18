package com.example.dailymovie.data

import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.WebService
import com.example.dailymovie.client.enqueueConvertido
import com.example.dailymovie.client.enqueueResultado
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.client.response.ResultadoMulti
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call

/**
 * La implementacion de verdad: habla con TMDB.
 *
 * La clave de la API se queda aqui dentro. Antes la pasaba cada llamante, asi que cualquier
 * pantalla tenia que saber de ella; ahora los ViewModels solo piden peliculas.
 *
 * @param servicio quien habla con TMDB. Se puede cambiar por un doble en los tests, que es
 *   justo para lo que existe la interfaz.
 */
class TmdbMovieRepository(
    private val servicio: WebService = RetrofitClient.webService
) : MovieRepository {

    override fun buscarTodo(
        consulta: String,
        pagina: Int,
        alTerminar: (Resultado<Pagina>) -> Unit
    ) {
        servicio.buscarTodo(consulta, page = pagina).enqueueSimple(
            onExito = { respuesta ->
                val resultados = Pagina(
                    hallazgos = aHallazgos(respuesta.resultados),
                    pagina = respuesta.pagina,
                    hayMas = respuesta.pagina < respuesta.totalDePaginas
                )
                // Las sagas van en otro endpoint y no estan paginadas, asi que solo se piden
                // la primera vez. Si fallan no pasa nada: se devuelve lo que haya llegado.
                if (resultados.pagina > 1) {
                    alTerminar(Resultado.Exito(resultados))
                    return@enqueueSimple
                }
                servicio.buscarSagas(consulta).enqueueSimple(
                    onExito = { sagas ->
                        val encontradas = sagas.resultados.take(3).map { Hallazgo.de(it) }
                        alTerminar(
                            Resultado.Exito(
                                resultados.copy(hallazgos = encontradas + resultados.hallazgos)
                            )
                        )
                    },
                    onError = { alTerminar(Resultado.Exito(resultados)) }
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun peliculasDeLaSaga(
        sagaId: Int,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        servicio.getSaga(sagaId).enqueueSimple(
            onExito = { saga ->
                alTerminar(
                    Resultado.Exito(
                        saga.peliculas.map {
                            MovieModel(
                                id = it.id,
                                title = it.titulo.orEmpty(),
                                releaseDate = it.estreno.orEmpty(),
                                voteAverage = it.nota,
                                posterPath = it.cartel
                            )
                        }
                    )
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit) {
        servicio.getTendencias().enqueueConvertido({ aHallazgos(it.resultados) }, alTerminar)
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
                    // search/multi no devuelve sagas: llegan del endpoint de colecciones y
                    // se construyen aparte, con su propia fabrica.
                    TipoDeHallazgo.SAGA -> ""
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
        listaDePeliculas(servicio.getNowPlayingMovies(), alTerminar)

    override fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getPopularMovies(), alTerminar)

    override fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getTopRatedMovies(), alTerminar)

    override fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getUpcomingMovies(), alTerminar)

    override fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getSimilarMovies(peliculaId), alTerminar)

    override fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) =
        listaDePeliculas(servicio.getRecommendedMovies(peliculaId), alTerminar)

    override fun porGeneros(
        generos: List<Int>,
        pagina: Int,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        // TMDB espera los generos separados por coma; con "|" seria "cualquiera de ellos"
        // y con "," "todos a la vez". Se usa la coma para afinar mas.
        listaDePeliculas(
            servicio.descubrirPeliculas(generos.joinToString(","), page = pagina),
            alTerminar
        )
    }

    override fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit) {
        servicio.getMovieDetails(peliculaId).enqueueResultado(alTerminar)
    }

    override fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) {
        servicio.getMovieProviders(peliculaId).enqueueResultado(alTerminar)
    }

    override fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) {
        servicio.getMovieCredits(peliculaId).enqueueResultado(alTerminar)
    }

    override fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) {
        // Se piden dos veces, en el idioma del dispositivo y en ingles, porque TMDB tiene
        // muchos mas trailers en ingles. Si la segunda falla nos quedamos con la primera.
        servicio.getMovieVideos(peliculaId, LocaleUtil.getLanguageAndCountry()).enqueueSimple(
            onExito = { enIdiomaLocal ->
                val locales = trailersPrimero(enIdiomaLocal.results)
                servicio.getMovieVideos(peliculaId, "en-US").enqueueSimple(
                    onExito = { alTerminar(Resultado.Exito(locales + trailersPrimero(it.results))) },
                    onError = { alTerminar(Resultado.Exito(locales)) }
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun resenas(peliculaId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit) {
        servicio.getResenas(peliculaId).enqueueSimple(
            onExito = { respuesta ->
                alTerminar(Resultado.Exito(respuesta.resultados.filter { it.texto.isNotBlank() }))
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun deLaGente(
        personas: List<Int>,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        listaDePeliculas(
            // Con "|" vale con que salga cualquiera de ellos; con "," tendrian que estar
            // todos en la misma pelicula, que casi nunca pasa.
            servicio.descubrirPeliculas(gente = personas.joinToString("|")),
            alTerminar
        )
    }

    override fun descubrir(
        generos: List<Int>,
        filtros: FiltrosAvanzados,
        pagina: Int,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        listaDePeliculas(
            servicio.descubrirPeliculas(
                generos = generos.takeIf { it.isNotEmpty() }?.joinToString(","),
                notaMinima = filtros.notaMinima,
                ano = filtros.ano,
                plataformas = filtros.plataforma?.toString(),
                // TMDB exige la region cuando se filtra por plataforma: las mismas no estan
                // en todos los paises y sin region no sabe de cual le hablas.
                region = filtros.plataforma?.let { LocaleUtil.getDeviceCountry() },
                page = pagina
            ),
            alTerminar
        )
    }

    override fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit) {
        servicio.getPlataformasDisponibles().enqueueConvertido({ it.plataformas.sortedBy { p -> p.prioridad } }, alTerminar)
    }

    override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) {
        servicio.getGeneros().enqueueConvertido({ it.generos }, alTerminar)
    }

    /** Los trailers van delante; el resto (clips, featurettes) detras. */
    private fun trailersPrimero(videos: List<VideoModel>): List<VideoModel> =
        videos.filter { it.type == "Trailer" } + videos.filter { it.type != "Trailer" }

    private fun listaDePeliculas(
        llamada: Call<com.example.dailymovie.client.response.MoviesResponse>,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    ) {
        llamada.enqueueConvertido({ it.results }, alTerminar)
    }
}
