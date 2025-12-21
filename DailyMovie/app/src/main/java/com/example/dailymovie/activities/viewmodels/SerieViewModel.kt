package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
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

    /**
     * Los episodios que el usuario ya ha visto, como pares (temporada, episodio).
     *
     * Se cargan una sola vez al abrir la serie y se van cambiando en memoria: pedirlos otra
     * vez a Firestore cada vez que se marca uno haria que el ojo tardara en cambiar.
     */
    private val _episodiosVistos = MutableLiveData<Set<Pair<Int, Int>>>(emptySet())
    val episodiosVistos: LiveData<Set<Pair<Int, Int>>> get() = _episodiosVistos

    private val _reparto = MutableLiveData<CreditResponse>()
    val reparto: LiveData<CreditResponse> get() = _reparto

    private val _videos = MutableLiveData<List<VideoModel>>()
    val videos: LiveData<List<VideoModel>> get() = _videos

    private val _plataformas = MutableLiveData<ProviderResponse>()
    val plataformas: LiveData<ProviderResponse> get() = _plataformas

    private val _clasificacionPorEdad = MutableLiveData<String?>()
    val clasificacionPorEdad: LiveData<String?> get() = _clasificacionPorEdad

    private val _resenas = MutableLiveData<List<ResenaResponse>>()
    val resenas: LiveData<List<ResenaResponse>> get() = _resenas

    private val _similares = MutableLiveData<List<SerieModel>>(emptyList())
    val similares: LiveData<List<SerieModel>> get() = _similares

    private val _recomendadas = MutableLiveData<List<SerieModel>>(emptyList())
    val recomendadas: LiveData<List<SerieModel>> get() = _recomendadas

    private val _imagenes = MutableLiveData<List<String>>(emptyList())
    val imagenes: LiveData<List<String>> get() = _imagenes

    /**
     * La serie que hay abierta, en la version corta que se guarda en Firestore.
     *
     * La necesitan los dos marcadores para saber que guardar; hasta que no se rellena en
     * [prepararBotonesDeGuardado] los botones no hacen nada.
     */
    private var serieAbierta: SerieModel? = null

    /** El corazon. Ver [Marcado] para por que esto no es un simple booleano. */
    val favorita = Marcado { comoQuedar, alTerminar ->
        serieAbierta?.let { usuario.ponerSerieFavorita(it, comoQuedar, alTerminar) }
            ?: alTerminar(false)
    }

    /** El ojo, igual que [favorita]. */
    val vista = Marcado { comoQuedar, alTerminar ->
        serieAbierta?.let { usuario.ponerSerieVista(it, comoQuedar, alTerminar) }
            ?: alTerminar(false)
    }

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
        series.clasificacionPorEdad(id) { _clasificacionPorEdad.value = it }
        usuario.episodiosVistos(id) { _episodiosVistos.value = it }
        // Ninguna de estas tres cuenta como error: son secciones que se esconden si no hay nada.
        series.similares(id) { if (it is Resultado.Exito) _similares.value = it.datos }
        series.recomendadas(id) { if (it is Resultado.Exito) _recomendadas.value = it.datos }
        series.imagenes(id) { _imagenes.value = it }
        // Las reseñas no cuentan como error si fallan: la ficha se ve igual de bien sin ellas.
        series.resenas(id) { if (it is Resultado.Exito) _resenas.value = it.datos }
    }

    /** Los episodios se piden solo de la temporada que el usuario abre, no de todas. */
    fun abrirTemporada(numero: Int) {
        if (serieId == -1) return
        _temporadaAbierta.value = null
        series.temporada(serieId, numero) { resultado ->
            if (resultado is Resultado.Exito) _temporadaAbierta.value = resultado.datos
        }
    }

    /**
     * Marca o desmarca un episodio.
     *
     * @param temporada el numero de temporada de TMDB.
     * @param episodio el numero dentro de esa temporada.
     */
    fun cambiarEpisodioVisto(temporada: Int, episodio: Int) {
        if (serieId == -1) return
        usuario.cambiarEpisodioVisto(serieId, temporada, episodio) { quedaVisto ->
            val actuales = _episodiosVistos.value.orEmpty().toMutableSet()
            if (quedaVisto) actuales.add(temporada to episodio) else actuales.remove(temporada to episodio)
            _episodiosVistos.value = actuales
        }
    }

    /** Sin sesion no hay donde guardar lo que se marca, asi que la pantalla lo esconde. */
    fun haySesion() = usuario.haySesion()

    /** Cuantos episodios de esa temporada ha visto ya. */
    fun cuantosVistosDe(temporada: Int) =
        _episodiosVistos.value.orEmpty().count { it.first == temporada }

    fun errorMostrado() {
        _error.value = null
    }

    // ---- Guardar la serie ----
    //
    // Los mismos gestos que en la ficha de pelicula: marcarla como favorita, como vista o
    // meterla en una lista. Las series se guardan en sus propios campos, ver UserRepository.

    /**
     * Prepara los dos botones de guardar de la ficha.
     *
     * Igual que en la ficha de pelicula: se pregunta una vez como esta y desde ahi manda la
     * memoria, no Firestore.
     *
     * @param serie la serie abierta en su version corta, que es lo que se guarda.
     */
    fun prepararBotonesDeGuardado(serie: SerieModel) {
        serieAbierta = serie
        usuario.esSerieFavorita(serie.id) { favorita.empezarEn(it) }
        usuario.estaSerieVista(serie.id) { vista.empezarEn(it) }
    }

    /** Le da la vuelta a "favorita". */
    fun cambiarFavorita() = favorita.cambiar { _error.value = ErrorCarga.SIN_CONEXION }

    /** Le da la vuelta a "vista". */
    fun cambiarVista() = vista.cambiar { _error.value = ErrorCarga.SIN_CONEXION }

    fun listasDelUsuario(alTerminar: (List<String>) -> Unit) =
        usuario.listasDelUsuario(alTerminar)

    fun listasConLaSerie(serieId: Int, alTerminar: (Set<String>) -> Unit) =
        usuario.listasQueContienenSerie(serieId, alTerminar)

    fun anadirALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.anadirSerieALista(nombre, serie, alTerminar)

    fun quitarDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        usuario.quitarSerieDeLista(nombre, serie, alTerminar)
}
