package com.example.dailymovie.viewmodels

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

    private val _customLists = MutableLiveData<List<ListaModel>>()
    val customLists: LiveData<List<ListaModel>> get() = _customLists

    private val _favorites = MutableLiveData<List<MovieModel>>()
    val favorites: LiveData<List<MovieModel>> get() = _favorites

    private val _watched = MutableLiveData<List<MovieModel>>()
    val watched: LiveData<List<MovieModel>> get() = _watched

    init {
        loadFavoriteAndWatchedLists()
        loadCustomLists()
    }

    private fun loadFavoriteAndWatchedLists() {
        val listas = listOf(
            ListaModel("Favoritos", R.drawable.ic_baseline_favorite_24),
            ListaModel("Vistos", R.drawable.ic_baseline_visibility_24)
        )
        _favoriteAndWatchedLists.value = listas
    }

    private fun loadCustomLists() {
        FirebaseClient.getCustomLists { listNames ->
            val customListModels = listNames.map { ListaModel(it, R.drawable.ic_baseline_list_24) }
            _customLists.value = customListModels
        }
    }

    fun createNewList(listName: String, onComplete: (Boolean) -> Unit) {
        FirebaseClient.createCustomList(listName) { success ->
            if (success) {
                loadCustomLists()
            }
            onComplete(success)
        }
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

    fun getMoviesFromList(listName: String, onComplete: (List<MovieModel>) -> Unit) {
        FirebaseClient.getMoviesFromList(listName) { movieList ->
            onComplete(movieList)
        }
    }

    fun deleteCustomList(listName: String, onComplete: (Boolean) -> Unit) {
        FirebaseClient.deleteCustomList(listName) { success ->
            if (success) {
                loadCustomLists()
            }
            onComplete(success)
        }
    }
}
