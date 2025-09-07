package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.ErrorCarga
import com.example.dailymovie.utils.LocaleUtil

class HomeViewModel : ViewModel() {

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

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    /** Peticiones que faltan por contestar en la carga actual. */
    private var peticionesPendientes = 0

    fun fetchNowPlayingMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getNowPlayingMovies(apiKey, language, page).enqueueSimple(
            onExito = { _nowPlayingMovies.value = it.results; peticionTerminada() },
            onError = { _error.value = it; peticionTerminada() }
        )
    }

    fun fetchPopularMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getPopularMovies(apiKey, language, page).enqueueSimple(
            onExito = { _popularMovies.value = it.results; peticionTerminada() },
            onError = { _error.value = it; peticionTerminada() }
        )
    }

    fun fetchTopRatedMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getTopRatedMovies(apiKey, language, page).enqueueSimple(
            onExito = { _topRatedMovies.value = it.results; peticionTerminada() },
            onError = { _error.value = it; peticionTerminada() }
        )
    }

    fun fetchUpcomingMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getUpcomingMovies(apiKey, language, page).enqueueSimple(
            onExito = { _upcomingMovies.value = it.results; peticionTerminada() },
            onError = { _error.value = it; peticionTerminada() }
        )
    }

    fun fetchMovieOfTheDay() {
        // Si nadie ha curado la pelicula de hoy en Firebase se enseña la de reserva, para
        // que la portada nunca quede vacia. Cuando exista la recomendacion segun gustos,
        // esta sera la que ocupe ese hueco y la de reserva desaparecera.
        FirebaseClient.getMovieOfTheDay { movie ->
            _movieOfTheDay.value = movie ?: PELICULA_DE_RESERVA
            peticionTerminada()
        }
    }

    /**
     * Pide de golpe todo lo que se ve en la portada y avisa mientras dure.
     * Lo llaman tanto la primera carga como el gesto de deslizar para refrescar.
     */
    fun cargarPortada(apiKey: String) {
        peticionesPendientes = TOTAL_PETICIONES
        _cargando.value = true

        fetchNowPlayingMovies(apiKey)
        fetchPopularMovies(apiKey)
        fetchTopRatedMovies(apiKey)
        fetchUpcomingMovies(apiKey)
        fetchMovieOfTheDay()
    }

    /**
     * Va descontando peticiones y apaga el indicador cuando han contestado todas, hayan
     * ido bien o mal. Antes el circulo de refrescar se paraba al instante, asi que el
     * gesto parecia terminado cuando en realidad no habia llegado nada.
     */
    private fun peticionTerminada() {
        peticionesPendientes--
        if (peticionesPendientes <= 0) {
            peticionesPendientes = 0
            _cargando.value = false
        }
    }

    /** La vista avisa de que ya ha enseñado el aviso, para que no vuelva a salir solo. */
    fun errorMostrado() {
        _error.value = null
    }

    private companion object {
        /** Las cuatro listas de TMDB mas la pelicula del dia. */
        const val TOTAL_PETICIONES = 5

        /**
         * Pelicula fija que se enseña mientras no haya recomendacion personalizada.
         * El id y el video son los reales de TMDB, asi que "Ver ficha completa" funciona.
         */
        val PELICULA_DE_RESERVA = MovieOfTheDay(
            id = 238,
            title = "El Padrino",
            review = "Don Vito Corleone maneja una de las familias mas poderosas de Nueva " +
                "York, y lo que empieza como una boda acaba siendo el retrato de como el " +
                "poder se hereda. Coppola la convirtio en la medida con la que se comparan " +
                "las demas peliculas de mafia. Si solo vas a ver una, que sea esta.",
            date = "",
            author = "DailyMovie",
            videoId = "v72XprPxy3E"
        )
    }
}
