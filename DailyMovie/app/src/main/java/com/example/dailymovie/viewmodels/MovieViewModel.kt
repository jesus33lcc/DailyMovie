package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.models.MovieModel

class MovieViewModel : ViewModel() {

    private val _movie = MutableLiveData<MovieModel>()
    val movie: LiveData<MovieModel> get() = _movie

    fun setMovie(movie: MovieModel) {
        _movie.value = movie
    }
}