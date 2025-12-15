package com.example.dailymovie.data

import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.WebService
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ImagenesResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call

/**
 * Todo lo de series.
 *
 * Va aparte del de peliculas aunque TMDB use endpoints parecidos, porque los datos no son
 * iguales: una serie tiene temporadas y episodios, y hasta el titulo se llama distinto
 * ("name" en vez de "title").
 */
interface SerieRepository {

    /**
     * Busca series por titulo.
     *
     * @param consulta lo que ha escrito el usuario.
     * @param alTerminar recibe las series que encajan, o un fallo si no hubo red o TMDB
     *   contesto con error. Que no haya coincidencias no es un fallo: llega una lista vacia.
     */
    fun buscar(consulta: String, alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * Las series mas vistas del momento.
     *
     * @param alTerminar recibe las series, o un fallo si no se pudo pedir.
     */
    fun populares(alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * Las series mejor valoradas segun los votos de TMDB.
     *
     * @param alTerminar recibe las series, o un fallo si no se pudo pedir.
     */
    fun mejorValoradas(alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * Las series que estan emitiendo capitulos estos dias.
     *
     * @param alTerminar recibe las series, o un fallo si no se pudo pedir.
     */
    fun enEmision(alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * La ficha de una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe los detalles con las temporadas solo enumeradas, sin los
     *   episodios dentro: cada temporada se pide aparte con [temporada].
     */
    fun detalles(serieId: Int, alTerminar: (Resultado<SerieDetailsResponse>) -> Unit)

    /**
     * Una temporada con todos sus episodios.
     *
     * @param serieId el id de TMDB de la serie.
     * @param numero el numero de temporada que da TMDB. La 0 existe y son los especiales, asi
     *   que no se puede dar por hecho que la primera sea la 1.
     * @param alTerminar recibe la temporada con sus episodios, o un fallo si no se pudo pedir.
     */
    fun temporada(serieId: Int, numero: Int, alTerminar: (Resultado<SeasonResponse>) -> Unit)

    /**
     * Quien sale y quien trabaja en una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe el reparto y el equipo por separado, contando toda la emision y
     *   no un capitulo suelto.
     */
    fun reparto(serieId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit)

    /**
     * Los videos de una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe los videos con los trailers delante y el resto detras. A
     *   diferencia de las peliculas, aqui no se pide una segunda vuelta en ingles.
     */
    fun videos(serieId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit)

    /**
     * Donde se puede ver una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe las plataformas de todos los paises a la vez; hay que quedarse
     *   con las del pais del aparato. Que ese pais no venga no es un fallo: es que ahi no esta.
     */
    fun plataformas(serieId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit)

    /**
     * La clasificacion por edad de la serie en el pais del usuario.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe la etiqueta ("16", "TV-MA") o null si esa serie no esta
     *   clasificada aqui. Se manda null en vez de la de otro pais porque una clasificacion
     *   extranjera confunde mas de lo que aclara.
     */
    fun clasificacionPorEdad(serieId: Int, alTerminar: (String?) -> Unit)

    /**
     * Series parecidas a una.
     *
     * @param serieId el id de TMDB de la serie de partida.
     * @param alTerminar recibe las parecidas, o un fallo si no se pudo pedir.
     */
    fun similares(serieId: Int, alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * Las que TMDB recomienda a quien ve esa serie.
     *
     * @param serieId el id de TMDB de la serie de partida.
     * @param alTerminar recibe las recomendadas, o un fallo si no se pudo pedir.
     */
    fun recomendadas(serieId: Int, alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    /**
     * Los fotogramas que TMDB tiene de una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe las rutas, sin la direccion delante. Llega vacio si no hay
     *   ninguna o si falla: una galeria de mas o de menos no es un error que enseñar.
     */
    fun imagenes(serieId: Int, alTerminar: (List<String>) -> Unit)

    /**
     * Las reseñas que ha escrito la gente sobre la serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe las reseñas con texto; las vacias se tiran.
     */
    fun resenas(serieId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit)

    /**
     * Los generos de series, que tienen sus propios ids distintos a los de cine.
     *
     * @param alTerminar recibe los generos con el nombre ya en el idioma del aparato. Estos
     *   ids solo valen para [porGeneros] de series; en cine dan cualquier cosa.
     */
    fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit)

    /**
     * Series de un genero, ordenadas por popularidad.
     *
     * @param generos ids de genero de series, no los de cine.
     * @param pagina cual de las tandas de 20 se pide.
     * @param alTerminar recibe las series, o un fallo si no se pudo pedir.
     */
    fun porGeneros(
        generos: List<Int>,
        pagina: Int = 1,
        alTerminar: (Resultado<List<SerieModel>>) -> Unit
    )
}

/**
 * La implementacion de verdad: pide las series a TMDB.
 *
 * @param servicio quien habla con TMDB. Se puede cambiar por un doble en los tests.
 * @param apiKey la clave de TMDB. Se lee aqui, en el constructor, para que nadie de las capas
 *   de arriba tenga que conocerla ni pasarla.
 */
class TmdbSerieRepository(
    private val servicio: WebService = RetrofitClient.webService,
    private val apiKey: String = Constantes.API_KEY
) : SerieRepository {

    override fun buscar(consulta: String, alTerminar: (Resultado<List<SerieModel>>) -> Unit) =
        listaDeSeries(servicio.buscarSeries(consulta, apiKey), alTerminar)

    override fun populares(alTerminar: (Resultado<List<SerieModel>>) -> Unit) =
        listaDeSeries(servicio.getSeriesPopulares(apiKey), alTerminar)

    override fun mejorValoradas(alTerminar: (Resultado<List<SerieModel>>) -> Unit) =
        listaDeSeries(servicio.getSeriesMejorValoradas(apiKey), alTerminar)

    override fun enEmision(alTerminar: (Resultado<List<SerieModel>>) -> Unit) =
        listaDeSeries(servicio.getSeriesEnEmision(apiKey), alTerminar)

    override fun detalles(serieId: Int, alTerminar: (Resultado<SerieDetailsResponse>) -> Unit) {
        servicio.getSerieDetalles(serieId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun temporada(serieId: Int, numero: Int, alTerminar: (Resultado<SeasonResponse>) -> Unit) {
        servicio.getTemporada(serieId, numero, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun reparto(serieId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) {
        servicio.getSerieCreditos(serieId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun videos(serieId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) {
        servicio.getSerieVideos(serieId, apiKey).enqueueSimple(
            onExito = { respuesta ->
                val trailersPrimero = respuesta.results.filter { it.type == "Trailer" } +
                    respuesta.results.filter { it.type != "Trailer" }
                alTerminar(Resultado.Exito(trailersPrimero))
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun similares(serieId: Int, alTerminar: (Resultado<List<SerieModel>>) -> Unit) {
        servicio.getSeriesSimilares(serieId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.results)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun recomendadas(serieId: Int, alTerminar: (Resultado<List<SerieModel>>) -> Unit) {
        servicio.getSeriesRecomendadas(serieId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.results)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun imagenes(serieId: Int, alTerminar: (List<String>) -> Unit) {
        servicio.getImagenesDeSerie(serieId, apiKey).enqueueSimple(
            // Los fondos y no los carteles: son fotogramas de la serie, que es lo que
            // apetece mirar, y ademas cuadran con la tira apaisada.
            onExito = { respuesta -> alTerminar(respuesta.fondos.map { it.ruta }) },
            onError = { alTerminar(emptyList()) }
        )
    }

    override fun clasificacionPorEdad(serieId: Int, alTerminar: (String?) -> Unit) {
        servicio.getClasificacionSerie(serieId, apiKey).enqueueSimple(
            onExito = { respuesta ->
                alTerminar(
                    respuesta.resultados
                        .firstOrNull { it.pais == LocaleUtil.getDeviceCountry() }
                        ?.clasificacion
                        ?.takeIf { it.isNotBlank() }
                )
            },
            // Sin clasificacion la ficha se ve igual de bien, asi que un fallo aqui no se
            // convierte en un error para el usuario.
            onError = { alTerminar(null) }
        )
    }

    override fun resenas(serieId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit) {
        servicio.getResenasSerie(serieId, apiKey).enqueueSimple(
            onExito = { respuesta ->
                alTerminar(Resultado.Exito(respuesta.resultados.filter { it.texto.isNotBlank() }))
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun plataformas(serieId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) {
        servicio.getSerieProviders(serieId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    private fun listaDeSeries(
        llamada: Call<com.example.dailymovie.client.response.SeriesResponse>,
        alTerminar: (Resultado<List<SerieModel>>) -> Unit
    ) {
        llamada.enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.results)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) {
        servicio.getGenerosDeSeries(apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it.generos)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun porGeneros(
        generos: List<Int>,
        pagina: Int,
        alTerminar: (Resultado<List<SerieModel>>) -> Unit
    ) = listaDeSeries(
        servicio.descubrirSeries(apiKey, generos.joinToString(","), page = pagina),
        alTerminar
    )
}
