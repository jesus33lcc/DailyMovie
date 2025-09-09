package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.ErrorCarga

class ExplorarViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _movies = MutableLiveData<List<MovieModel>>()
    val movies: LiveData<List<MovieModel>> get() = _movies

    private val _history = MutableLiveData<List<MovieModel>>()
    val history: LiveData<List<MovieModel>> get() = _history

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    fun searchMovies(consulta: String) {
        peliculas.buscar(consulta) { resultado ->
            when (resultado) {
                is Resultado.Exito -> _movies.value = resultado.datos
                is Resultado.Fallo -> {
                    // Antes se vaciaba la lista sin decir nada, asi que un fallo de red se
                    // veia igual que una busqueda sin resultados.
                    _movies.value = emptyList()
                    _error.value = resultado.motivo
                }
            }
        }
    }

    fun addToHistory(pelicula: MovieModel) {
        usuario.anadirAlHistorial(pelicula) { }
    }

    fun loadHistory() {
        usuario.historial { _history.value = it }
    }

    fun errorMostrado() {
        _error.value = null
    }
}
