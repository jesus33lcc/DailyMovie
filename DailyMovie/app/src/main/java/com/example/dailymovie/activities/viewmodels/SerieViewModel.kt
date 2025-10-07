package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.SerieRepository
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga

class SerieViewModel(
    private val series: SerieRepository = Dependencias.series,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _detalles = MutableLiveData<SerieDetailsResponse>()
    val detalles: LiveData<SerieDetailsResponse> get() = _detalles

    private val _temporadaAbierta = MutableLiveData<SeasonResponse?>()
    val temporadaAbierta: LiveData<SeasonResponse?> get() = _temporadaAbierta

    private val _reparto = MutableLiveData<CreditResponse>()
    val reparto: LiveData<CreditResponse> get() = _reparto

    private val _videos = MutableLiveData<List<VideoModel>>()
    val videos: LiveData<List<VideoModel>> get() = _videos

    private val _plataformas = MutableLiveData<ProviderResponse>()
    val plataformas: LiveData<ProviderResponse> get() = _plataformas

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    private var serieId = -1

    fun cargar(id: Int) {
        serieId = id
        series.detalles(id) { resultado ->
            when (resultado) {
                is Resultado.Exito -> {
                    _detalles.value = resultado.datos
                    // Se abre sola la primera temporada de verdad. La 0 son los especiales,
                    // que no es por donde empieza nadie una serie.
                    resultado.datos.temporadas
                        .firstOrNull { it.numero > 0 }
                        ?.let { abrirTemporada(it.numero) }
                }
                is Resultado.Fallo -> _error.value = resultado.motivo
            }
        }
        series.reparto(id) { if (it is Resultado.Exito) _reparto.value = it.datos }
        series.videos(id) { if (it is Resultado.Exito) _videos.value = it.datos }
        series.plataformas(id) { if (it is Resultado.Exito) _plataformas.value = it.datos }
    }

    /** Los episodios se piden solo de la temporada que el usuario abre, no de todas. */
    fun abrirTemporada(numero: Int) {
        if (serieId == -1) return
        _temporadaAbierta.value = null
        series.temporada(serieId, numero) { resultado ->
            if (resultado is Resultado.Exito) _temporadaAbierta.value = resultado.datos
        }
    }

    fun errorMostrado() {
        _error.value = null
    }

    // ---- Guardar la serie ----
    //
    // Los mismos gestos que en la ficha de pelicula: marcarla como favorita, como vista o
    // meterla en una lista. Las series se guardan en sus propios campos, ver UserRepository.

    fun esFavorita(serieId: Int, alTerminar: (Boolean) -> Unit) =
        usuario.esSerieFavorita(serieId, alTerminar)

    fun estaVista(serieId: Int, alTerminar: (Boolean) -> Unit) =
        usuario.estaSerieVista(serieId, alTerminar)

    fun cambiarFavorita(serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.cambiarSerieFavorita(serie, alTerminar)

    fun cambiarVista(serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.cambiarSerieVista(serie, alTerminar)

    fun listasDelUsuario(alTerminar: (List<String>) -> Unit) =
        usuario.listasDelUsuario(alTerminar)

    fun listasConLaSerie(serieId: Int, alTerminar: (Set<String>) -> Unit) =
        usuario.listasQueContienenSerie(serieId, alTerminar)

    fun anadirALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.anadirSerieALista(nombre, serie, alTerminar)

    fun quitarDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.quitarSerieDeLista(nombre, serie, alTerminar)
}
