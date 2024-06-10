package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.response.MoviesResponse
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExplorarViewModel : ViewModel() {

    private val _movies = MutableLiveData<List<MovieModel>>()
    val movies: LiveData<List<MovieModel>> get() = _movies

    private var isSearching = false

    fun searchMovies(query: String) {
        isSearching = true
        RetrofitClient.webService.searchMovies(query, Constantes.API_KEY, true, LocaleUtil.getLanguageAndCountry(), 1)
            .enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _movies.value = response.body()?.results ?: emptyList()
                    } else {
                        _movies.value = emptyList()
                    }
                }

                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    _movies.value = emptyList()
                }
            })
    }

    fun addToHistory(movie: MovieModel) {
        FirebaseClient.addToHistory(movie) { success ->
        }
    }

    fun loadHistory() {
        if (!isSearching) {
            FirebaseClient.getHistory { movies ->
                _movies.value = movies
            }
        }
    }

    fun clearMovies() {
        _movies.value = emptyList()
    }

    fun setSearchMode(searching: Boolean) {
        isSearching = searching
    }
}
