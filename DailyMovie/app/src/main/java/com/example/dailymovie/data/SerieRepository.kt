package com.example.dailymovie.data

import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.WebService
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Constantes
import retrofit2.Call

/**
 * Todo lo de series.
 *
 * Va aparte del de peliculas aunque TMDB use endpoints parecidos, porque los datos no son
 * iguales: una serie tiene temporadas y episodios, y hasta el titulo se llama distinto
 * ("name" en vez de "title").
 */
interface SerieRepository {
    fun buscar(consulta: String, alTerminar: (Resultado<List<SerieModel>>) -> Unit)
    fun populares(alTerminar: (Resultado<List<SerieModel>>) -> Unit)
    fun mejorValoradas(alTerminar: (Resultado<List<SerieModel>>) -> Unit)
    fun enEmision(alTerminar: (Resultado<List<SerieModel>>) -> Unit)

    fun detalles(serieId: Int, alTerminar: (Resultado<SerieDetailsResponse>) -> Unit)
    fun temporada(serieId: Int, numero: Int, alTerminar: (Resultado<SeasonResponse>) -> Unit)
    fun reparto(serieId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit)
    fun videos(serieId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit)
    fun plataformas(serieId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit)

    /** Los generos de series, que tienen sus propios ids distintos a los de cine. */
    fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit)

    /** Series de un genero, ordenadas por popularidad. */
    fun porGeneros(generos: List<Int>, alTerminar: (Resultado<List<SerieModel>>) -> Unit)
}

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

    override fun porGeneros(generos: List<Int>, alTerminar: (Resultado<List<SerieModel>>) -> Unit) =
        listaDeSeries(servicio.descubrirSeries(apiKey, generos.joinToString(",")), alTerminar)
}
