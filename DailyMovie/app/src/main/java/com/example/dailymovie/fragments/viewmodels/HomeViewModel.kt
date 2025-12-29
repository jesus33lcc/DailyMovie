package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Recomendador
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.data.SerieRepository
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.ErrorCarga
import java.util.Calendar

class HomeViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario,
    private val series: SerieRepository = Dependencias.series,
    private val recomendador: Recomendador = Recomendador()
) : ViewModel() {

    private val _nowPlayingMovies = MutableLiveData<List<MovieModel>>()
    val nowPlayingMovies: LiveData<List<MovieModel>> get() = _nowPlayingMovies
    private val _popularMovies = MutableLiveData<List<MovieModel>>()
    val popularMovies: LiveData<List<MovieModel>> get() = _popularMovies
    private val _topRatedMovies = MutableLiveData<List<MovieModel>>()
    val topRatedMovies: LiveData<List<MovieModel>> get() = _topRatedMovies
    private val _upcomingMovies = MutableLiveData<List<MovieModel>>()
    val upcomingMovies: LiveData<List<MovieModel>> get() = _upcomingMovies

    /**
     * Series que el usuario ha empezado y no ha terminado, con lo que lleva de cada una.
     *
     * Es la seccion "Sigue viendo": sin ella, marcar episodios no servia para nada fuera de
     * la propia ficha de la serie, y volver a encontrar donde lo dejaste obligaba a buscarla.
     */
    private val _seriesEmpezadas = MutableLiveData<List<SerieEmpezada>>(emptyList())
    val seriesEmpezadas: LiveData<List<SerieEmpezada>> get() = _seriesEmpezadas

    private val _movieOfTheDay = MutableLiveData<MovieOfTheDay?>()
    val movieOfTheDay: LiveData<MovieOfTheDay?> get() = _movieOfTheDay

    /** Por que se enseña esa pelicula, para poder explicarselo al usuario en la portada. */
    private val _motivoRecomendacion = MutableLiveData<String?>()
    val motivoRecomendacion: LiveData<String?> get() = _motivoRecomendacion

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    private var peticionesPendientes = 0

    /**
     * De que carga es cada respuesta.
     *
     * La portada se recarga al tirar hacia abajo, y antes eso reseteaba el contador a 5 con
     * hasta cinco respuestas de la carga anterior todavia en el aire: las cinco primeras que
     * llegaran apagaban la rueda con media portada sin pedir. Con el numero de carga, las
     * respuestas viejas ni se cuentan ni se pintan.
     */
    private var generacionDeCarga = 0

    /** Pide de golpe todo lo que se ve en la portada y avisa mientras dure. */
    fun cargarPortada() {
        val mia = ++generacionDeCarga
        // Se cuenta al lanzar cada peticion en vez de con una constante escrita a mano: antes
        // habia un TOTAL_PETICIONES = 5 que habia que acordarse de subir al añadir una fila,
        // y si no, la rueda se quedaba girando para siempre.
        peticionesPendientes = 0
        _cargando.value = true

        pedir(mia) { peliculas.enCartelera { r -> reparte(mia, r) { l -> _nowPlayingMovies.value = l } } }
        pedir(mia) { peliculas.populares { r -> reparte(mia, r) { l -> _popularMovies.value = l } } }
        pedir(mia) { peliculas.mejorValoradas { r -> reparte(mia, r) { l -> _topRatedMovies.value = l } } }
        pedir(mia) { peliculas.proximamente { r -> reparte(mia, r) { l -> _upcomingMovies.value = l } } }
        pedir(mia) { cargarPeliculaDestacada(mia) }
        cargarSeriesEmpezadas()
    }

    /** Apunta una peticion mas y la lanza. */
    private fun pedir(generacion: Int, peticion: () -> Unit) {
        if (generacion != generacionDeCarga) return
        peticionesPendientes++
        peticion()
    }

    /**
     * Busca las series a medias.
     *
     * Los detalles de cada una hay que pedirlos a TMDB porque en Firestore solo se guarda el
     * id: el titulo y el cartel cambian con el idioma del aparato y guardarlos ahi los dejaria
     * congelados en el idioma del dia que se marco el episodio.
     *
     * Se piden como mucho seis y no cuentan para el indicador de carga de la portada: son un
     * extra, y si tardan no deben hacer esperar a lo demas.
     */
    private fun cargarSeriesEmpezadas() {
        usuario.seriesEmpezadas { empezadas ->
            if (empezadas.isEmpty()) {
                _seriesEmpezadas.value = emptyList()
                return@seriesEmpezadas
            }
            val aConsultar = empezadas.entries.sortedByDescending { it.value }.take(MAXIMO_EN_CURSO)
            val encontradas = mutableListOf<SerieEmpezada>()
            var pendientes = aConsultar.size

            aConsultar.forEach { (serieId, vistos) ->
                series.detalles(serieId) { resultado ->
                    val detalles = (resultado as? Resultado.Exito)?.datos
                    // Las terminadas no salen: la seccion es para retomar, no para presumir.
                    if (detalles != null && vistos < detalles.numeroDeEpisodios) {
                        encontradas += SerieEmpezada(
                            serie = Hallazgo(
                                id = detalles.id,
                                titulo = detalles.titulo,
                                subtitulo = detalles.estreno.orEmpty(),
                                imagen = detalles.poster,
                                nota = detalles.valoracion,
                                tipo = TipoDeHallazgo.SERIE
                            ),
                            vistos = vistos,
                            total = detalles.numeroDeEpisodios
                        )
                    }
                    pendientes--
                    if (pendientes == 0) {
                        _seriesEmpezadas.value = encontradas.sortedByDescending { it.vistos }
                    }
                }
            }
        }
    }

    /**
     * Elige la pelicula de la portada, en este orden:
     *
     * 1. La curada a mano en Firebase para hoy, si la hay.
     * 2. Una recomendada segun los gustos que el usuario dio al registrarse.
     * 3. Si no ha dicho sus gustos, la mas popular del momento.
     *
     * La idea es que la portada nunca se quede vacia, y que cuanto mas sepa la app de ti,
     * mas tuya sea la pelicula que te enseña.
     */
    private fun cargarPeliculaDestacada(generacion: Int) {
        usuario.peliculaDelDia { curada ->
            if (generacion != generacionDeCarga) return@peliculaDelDia
            if (curada != null) {
                _motivoRecomendacion.value = null
                _movieOfTheDay.value = curada
                peticionTerminada(generacion)
            } else {
                recomendarSegunGustos(generacion)
            }
        }
    }

    /**
     * Pide una recomendacion al Recomendador, que es quien sabe combinar gustos, gente
     * seguida y favoritos.
     *
     * La semilla es el dia del año: la portada cambia de un dia para otro, pero no cada vez
     * que se abre la app. Si cambiara a cada rato no seria "la pelicula del dia" de nadie.
     */
    private fun recomendarSegunGustos(generacion: Int) {
        val diaDelAno = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        recomendador.paraHoy(diaDelAno) { propuesta ->
            if (generacion != generacionDeCarga) return@paraHoy
            if (propuesta == null) {
                _motivoRecomendacion.value = null
                _movieOfTheDay.value = null
                peticionTerminada(generacion)
            } else {
                publicarRecomendada(generacion, propuesta.pelicula, propuesta.motivo)
            }
        }
    }

    /**
     * Completa la pelicula recomendada con su sinopsis y su trailer.
     *
     * La curada a mano en Firebase trae reseña y video escritos por nosotros; una
     * recomendada no, asi que se rellena con lo que da TMDB para que la portada se vea
     * igual de completa venga de donde venga.
     */
    private fun publicarRecomendada(generacion: Int, pelicula: MovieModel, motivo: String) {
        peliculas.detalles(pelicula.id) { detalles ->
            if (generacion != generacionDeCarga) return@detalles
            val sinopsis = (detalles as? Resultado.Exito)?.datos?.overview.orEmpty()
            peliculas.videos(pelicula.id) { videos ->
                if (generacion != generacionDeCarga) return@videos
                val trailer = (videos as? Resultado.Exito)?.datos?.firstOrNull()?.key.orEmpty()
                _motivoRecomendacion.value = motivo
                _movieOfTheDay.value = MovieOfTheDay(
                    id = pelicula.id,
                    title = pelicula.title,
                    review = sinopsis,
                    date = "",
                    // Una recomendada no tiene autor: lo que tiene es un motivo.
                    motivo = motivo,
                    videoId = trailer
                )
                peticionTerminada(generacion)
            }
        }
    }

    private fun <T : Any> reparte(generacion: Int, resultado: Resultado<T>, alIrBien: (T) -> Unit) {
        if (generacion != generacionDeCarga) return
        when (resultado) {
            is Resultado.Exito -> alIrBien(resultado.datos)
            is Resultado.Fallo -> _error.value = resultado.motivo
        }
        peticionTerminada(generacion)
    }

    /** Apaga el indicador cuando han contestado todas, hayan ido bien o mal. */
    private fun peticionTerminada(generacion: Int) {
        if (generacion != generacionDeCarga) return
        peticionesPendientes--
        if (peticionesPendientes <= 0) {
            peticionesPendientes = 0
            _cargando.value = false
        }
    }

    fun errorMostrado() {
        _error.value = null
    }

    private companion object {
        /** Mas de seis y la fila se convierte en otra lista infinita, que no es la idea. */
        const val MAXIMO_EN_CURSO = 6
    }
}

/**
 * Una serie que el usuario ha empezado y no ha terminado.
 *
 * @property serie la serie, ya en el formato que pinta la tarjeta comun.
 * @property vistos cuantos episodios lleva marcados.
 * @property total cuantos tiene la serie entera, para poder decir "12 de 62".
 */
data class SerieEmpezada(
    val serie: Hallazgo,
    val vistos: Int,
    val total: Int
) {
    /** "12 de 62 episodios", lo que se enseña debajo del titulo de la seccion. */
    fun comoVas() = "$vistos de $total episodios"
}
