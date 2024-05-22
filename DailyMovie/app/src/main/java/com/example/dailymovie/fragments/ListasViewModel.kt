package com.example.dailymovie.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel

class ListasViewModel : ViewModel() {
    private val _favoriteAndWatchedLists = MutableLiveData<List<ListaModel>>()
    val favoriteAndWatchedLists: LiveData<List<ListaModel>> get() = _favoriteAndWatchedLists

    private val _favorites = MutableLiveData<List<MovieModel>>()
    val favorites: LiveData<List<MovieModel>> get() = _favorites

    private val _watched = MutableLiveData<List<MovieModel>>()
    val watched: LiveData<List<MovieModel>> get() = _watched
    init {
        loadFavoriteAndWatchedLists()
    }

    private fun loadFavoriteAndWatchedLists() {
        val listas = listOf(
            ListaModel("Favoritos", R.drawable.ic_fav),
            ListaModel("Vistos", R.drawable.ic_eye)
        )
        _favoriteAndWatchedLists.value = listas
    }
    fun getFavorites(onComplete: (List<MovieModel>) -> Unit) {
        FirebaseClient.getFavorites { movieList ->
            _favorites.value = movieList
            onComplete(movieList)
        }
    }

    fun getWatched(onComplete: (List<MovieModel>) -> Unit) {
        FirebaseClient.getWatched { movieList ->
            _watched.value = movieList
            onComplete(movieList)
        }
    }
}