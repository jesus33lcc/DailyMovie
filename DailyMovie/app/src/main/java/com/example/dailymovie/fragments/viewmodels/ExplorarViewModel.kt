package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.ErrorCarga
import com.example.dailymovie.utils.LocaleUtil

class ExplorarViewModel : ViewModel() {

    private val _movies = MutableLiveData<List<MovieModel>>()
    val movies: LiveData<List<MovieModel>> get() = _movies

    private val _history = MutableLiveData<List<MovieModel>>()
    val history: LiveData<List<MovieModel>> get() = _history

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    fun searchMovies(query: String) {
        RetrofitClient.webService
            .searchMovies(query, Constantes.API_KEY, true, LocaleUtil.getLanguageAndCountry(), 1)
            .enqueueSimple(
                onExito = { _movies.value = it.results },
                onError = {
                    // Antes se vaciaba la lista sin decir nada, asi que un fallo de red se
                    // veia igual que una busqueda sin resultados.
                    _movies.value = emptyList()
                    _error.value = it
                }
            )
    }

    fun addToHistory(movie: MovieModel) {
        FirebaseClient.addToHistory(movie) { success ->
        }
    }

    fun loadHistory() {
        FirebaseClient.getHistory { movies ->
            _history.value = movies
        }
    }

    /** La vista avisa de que ya ha enseñado el aviso, para que no vuelva a salir solo. */
    fun errorMostrado() {
        _error.value = null
    }
}
