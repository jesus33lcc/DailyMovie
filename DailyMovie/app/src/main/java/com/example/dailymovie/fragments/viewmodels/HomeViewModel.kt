package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Gustos
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.ErrorCarga

class HomeViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _nowPlayingMovies = MutableLiveData<List<MovieModel>>()
    val nowPlayingMovies: LiveData<List<MovieModel>> get() = _nowPlayingMovies
    private val _popularMovies = MutableLiveData<List<MovieModel>>()
    val popularMovies: LiveData<List<MovieModel>> get() = _popularMovies
    private val _topRatedMovies = MutableLiveData<List<MovieModel>>()
    val topRatedMovies: LiveData<List<MovieModel>> get() = _topRatedMovies
    private val _upcomingMovies = MutableLiveData<List<MovieModel>>()
    val upcomingMovies: LiveData<List<MovieModel>> get() = _upcomingMovies

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

    /** Pide de golpe todo lo que se ve en la portada y avisa mientras dure. */
    fun cargarPortada() {
        peticionesPendientes = TOTAL_PETICIONES
        _cargando.value = true

        peliculas.enCartelera { reparte(it) { lista -> _nowPlayingMovies.value = lista } }
        peliculas.populares { reparte(it) { lista -> _popularMovies.value = lista } }
        peliculas.mejorValoradas { reparte(it) { lista -> _topRatedMovies.value = lista } }
        peliculas.proximamente { reparte(it) { lista -> _upcomingMovies.value = lista } }
        cargarPeliculaDestacada()
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
    private fun cargarPeliculaDestacada() {
        usuario.peliculaDelDia { curada ->
            if (curada != null) {
                _motivoRecomendacion.value = null
                _movieOfTheDay.value = curada
                peticionTerminada()
            } else {
                recomendarSegunGustos()
            }
        }
    }

    private fun recomendarSegunGustos() {
        usuario.gustos { gustos ->
            if (gustos == null || gustos.generos.isEmpty()) {
                elegirEntrePopulares()
            } else {
                recomendarPorGeneros(gustos)
            }
        }
    }

    private fun recomendarPorGeneros(gustos: Gustos) {
        peliculas.porGeneros(gustos.generos) { resultado ->
            val candidata = (resultado as? Resultado.Exito)?.datos
                ?.firstOrNull { it.id !in gustos.peliculas }
            if (candidata == null) {
                elegirEntrePopulares()
            } else {
                publicarRecomendada(candidata, MOTIVO_GUSTOS)
            }
        }
    }

    private fun elegirEntrePopulares() {
        peliculas.populares { resultado ->
            val candidata = (resultado as? Resultado.Exito)?.datos?.firstOrNull()
            if (candidata == null) {
                _motivoRecomendacion.value = null
                _movieOfTheDay.value = null
                peticionTerminada()
            } else {
                publicarRecomendada(candidata, MOTIVO_POPULAR)
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
    private fun publicarRecomendada(pelicula: MovieModel, motivo: String) {
        peliculas.detalles(pelicula.id) { detalles ->
            val sinopsis = (detalles as? Resultado.Exito)?.datos?.overview.orEmpty()
            peliculas.videos(pelicula.id) { videos ->
                val trailer = (videos as? Resultado.Exito)?.datos?.firstOrNull()?.key.orEmpty()
                _motivoRecomendacion.value = motivo
                _movieOfTheDay.value = MovieOfTheDay(
                    id = pelicula.id,
                    title = pelicula.title,
                    review = sinopsis,
                    date = "",
                    author = motivo,
                    videoId = trailer
                )
                peticionTerminada()
            }
        }
    }

    private fun <T> reparte(resultado: Resultado<T>, alIrBien: (T) -> Unit) {
        when (resultado) {
            is Resultado.Exito -> alIrBien(resultado.datos)
            is Resultado.Fallo -> _error.value = resultado.motivo
        }
        peticionTerminada()
    }

    /** Apaga el indicador cuando han contestado todas, hayan ido bien o mal. */
    private fun peticionTerminada() {
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
        const val TOTAL_PETICIONES = 5
        const val MOTIVO_GUSTOS = "Porque te gusta este tipo de cine"
        const val MOTIVO_POPULAR = "De lo más visto ahora mismo"
    }
}
