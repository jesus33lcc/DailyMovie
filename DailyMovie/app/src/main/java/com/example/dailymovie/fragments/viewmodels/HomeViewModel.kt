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

    fun fetchNowPlayingMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getNowPlayingMovies(apiKey, language, page).enqueueSimple(
            onExito = { _nowPlayingMovies.value = it.results },
            onError = { _error.value = it }
        )
    }

    fun fetchPopularMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getPopularMovies(apiKey, language, page).enqueueSimple(
            onExito = { _popularMovies.value = it.results },
            onError = { _error.value = it }
        )
    }

    fun fetchTopRatedMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getTopRatedMovies(apiKey, language, page).enqueueSimple(
            onExito = { _topRatedMovies.value = it.results },
            onError = { _error.value = it }
        )
    }

    fun fetchUpcomingMovies(apiKey: String, language: String = LocaleUtil.getLanguageAndCountry(), page: Int = 1) {
        RetrofitClient.webService.getUpcomingMovies(apiKey, language, page).enqueueSimple(
            onExito = { _upcomingMovies.value = it.results },
            onError = { _error.value = it }
        )
    }

    fun fetchMovieOfTheDay() {
        // Que venga null es normal: si nadie ha curado la pelicula de hoy en Firebase
        // simplemente no hay ninguna, y eso no es un error que enseñarle al usuario.
        FirebaseClient.getMovieOfTheDay { movie ->
            _movieOfTheDay.value = movie
        }
    }

    /** La vista avisa de que ya ha enseñado el aviso, para que no vuelva a salir solo. */
    fun errorMostrado() {
        _error.value = null
    }
}
