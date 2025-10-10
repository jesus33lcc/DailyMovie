package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.utils.ErrorCarga
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Como se filtran y ordenan los resultados. Un tipo a null es "todo". */
data class FiltroDeBusqueda(
    val tipo: TipoDeHallazgo? = null,
    val orden: OrdenDeBusqueda = OrdenDeBusqueda.RELEVANCIA
)

enum class OrdenDeBusqueda { RELEVANCIA, NOTA, ANO }

class ExplorarViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    /** Lo que devuelve TMDB, sin tocar. Los filtros se aplican encima al enseñarlo. */
    private var todosLosResultados: List<Hallazgo> = emptyList()

    private val _resultados = MutableLiveData<List<Hallazgo>>()
    val resultados: LiveData<List<Hallazgo>> get() = _resultados

    private val _tendencias = MutableLiveData<List<Hallazgo>>()
    val tendencias: LiveData<List<Hallazgo>> get() = _tendencias

    private val _generos = MutableLiveData<List<GenreModel>>()
    val generos: LiveData<List<GenreModel>> get() = _generos

    private val _history = MutableLiveData<List<MovieModel>>()
    val history: LiveData<List<MovieModel>> get() = _history

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    /** True cuando se ha buscado algo y no ha salido nada, para poder decirlo. */
    private val _sinResultados = MutableLiveData(false)
    val sinResultados: LiveData<Boolean> get() = _sinResultados

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    var filtro = FiltroDeBusqueda()
        private set

    private var consultaActual = ""
    private var trabajoDeBusqueda: Job? = null

    /**
     * Busca con freno.
     *
     * Antes salia una peticion por cada letra a partir de la tercera: escribir "interstellar"
     * eran nueve llamadas a TMDB, y como cada una tarda lo suyo, las respuestas podian llegar
     * desordenadas y pintarse los resultados de "inters" encima de los de "interstellar".
     *
     * Ahora cada tecla cancela la busqueda anterior y se espera un momento a que pares de
     * escribir: sale una sola peticion, y siempre la de lo ultimo que hay en el campo.
     */
    fun buscar(consulta: String) {
        val limpia = consulta.trim()
        consultaActual = limpia

        trabajoDeBusqueda?.cancel()

        if (limpia.length < MINIMO_PARA_BUSCAR) {
            todosLosResultados = emptyList()
            _resultados.value = emptyList()
            _sinResultados.value = false
            _cargando.value = false
            return
        }

        trabajoDeBusqueda = viewModelScope.launch {
            delay(ESPERA_ANTES_DE_BUSCAR)
            _cargando.value = true
            peliculas.buscarTodo(limpia) { resultado ->
                _cargando.value = false
                when (resultado) {
                    is Resultado.Exito -> {
                        todosLosResultados = resultado.datos
                        aplicarFiltro()
                        _sinResultados.value = resultado.datos.isEmpty()
                    }
                    is Resultado.Fallo -> {
                        todosLosResultados = emptyList()
                        _resultados.value = emptyList()
                        _sinResultados.value = false
                        _error.value = resultado.motivo
                    }
                }
            }
        }
    }

    fun cambiarFiltro(nuevo: FiltroDeBusqueda) {
        filtro = nuevo
        aplicarFiltro()
    }

    /** Los filtros se aplican sobre lo ya descargado: cambiar de chip no vuelve a la red. */
    private fun aplicarFiltro() {
        val porTipo = filtro.tipo?.let { t -> todosLosResultados.filter { it.tipo == t } }
            ?: todosLosResultados

        _resultados.value = when (filtro.orden) {
            OrdenDeBusqueda.RELEVANCIA -> porTipo
            OrdenDeBusqueda.NOTA -> porTipo.sortedByDescending { it.nota }
            OrdenDeBusqueda.ANO -> porTipo.sortedByDescending { it.ano }
        }
    }

    /** Cuantos hay de cada tipo, para poder ponerlo en los chips. */
    fun cuantosHayDe(tipo: TipoDeHallazgo?): Int =
        if (tipo == null) todosLosResultados.size
        else todosLosResultados.count { it.tipo == tipo }

    fun hayBusquedaEnMarcha() = consultaActual.length >= MINIMO_PARA_BUSCAR

    // ---- Lo que se enseña cuando no se ha escrito nada ----

    fun cargarTendencias() {
        if (_tendencias.value?.isNotEmpty() == true) return
        peliculas.tendencias { resultado ->
            if (resultado is Resultado.Exito) _tendencias.value = resultado.datos
        }
    }

    fun cargarGeneros() {
        if (_generos.value?.isNotEmpty() == true) return
        peliculas.generos { resultado ->
            if (resultado is Resultado.Exito) _generos.value = resultado.datos
        }
    }

    /**
     * Enseña lo mas popular de un genero como si fuera una busqueda.
     *
     * Reaprovecha discover, el mismo endpoint que alimenta la recomendacion de la portada.
     * Los resultados se meten en la misma rejilla, asi que tocar un genero se comporta igual
     * que escribir algo: mismos filtros y mismas tarjetas.
     */
    fun explorarGenero(genero: GenreModel) {
        trabajoDeBusqueda?.cancel()
        consultaActual = genero.name
        _cargando.value = true

        peliculas.porGeneros(listOf(genero.id)) { resultado ->
            _cargando.value = false
            if (resultado is Resultado.Exito) {
                todosLosResultados = resultado.datos.map { pelicula ->
                    Hallazgo(
                        id = pelicula.id,
                        titulo = pelicula.title,
                        subtitulo = pelicula.releaseDate,
                        imagen = pelicula.posterPath,
                        nota = pelicula.voteAverage,
                        tipo = TipoDeHallazgo.PELICULA
                    )
                }
                aplicarFiltro()
                _sinResultados.value = todosLosResultados.isEmpty()
            } else if (resultado is Resultado.Fallo) {
                _error.value = resultado.motivo
            }
        }
    }

    /** Una al azar de lo que esta en tendencia, para el "no se que ver". */
    fun unaAlAzar(): Hallazgo? = _tendencias.value?.randomOrNull()

    fun loadHistory() {
        usuario.historial { _history.value = it }
    }

    fun addToHistory(pelicula: MovieModel) {
        usuario.anadirAlHistorial(pelicula) { }
    }

    fun errorMostrado() {
        _error.value = null
    }

    private companion object {
        /** Con una sola letra cualquier busqueda devuelve ruido. */
        const val MINIMO_PARA_BUSCAR = 2
        const val ESPERA_ANTES_DE_BUSCAR = 350L
    }
}
