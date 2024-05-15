package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.models.MovieModel

class MovieViewModel : ViewModel() {

    private val _movie = MutableLiveData<MovieModel>()
    val movie: LiveData<MovieModel> get() = _movie

    fun setMovie(movie: MovieModel) {
        _movie.value = movie
    }
    fun addToFavorites(movieId: Int, onComplete: (Boolean) -> Unit) {
        FirebaseClient.addToFavorites(movieId, onComplete)
    }

    fun removeFromFavorites(movieId: Int, onComplete: (Boolean) -> Unit) {
        FirebaseClient.removeFromFavorites(movieId, onComplete)
    }

    fun addToWatched(movieId: Int, onComplete: (Boolean) -> Unit) {
        FirebaseClient.addToWatched(movieId, onComplete)
    }

    fun removeFromWatched(movieId: Int, onComplete: (Boolean) -> Unit) {
        FirebaseClient.removeFromWatched(movieId, onComplete)
    }
}