package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.models.MovieDetailsModel
import com.example.dailymovie.models.MovieModel
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieViewModel : ViewModel() {

    private val _movieDetails = MutableLiveData<MovieDetailsResponse>()
    val movieDetails: LiveData<MovieDetailsResponse> get() = _movieDetails
    private var currentMovieModel: MovieModel? = null

    fun setCurrentMovieModel(movieModel: MovieModel) {
        currentMovieModel = movieModel
    }

    fun fetchMovieDetails(movieId: Int, apiKey: String, language: String) {
        viewModelScope.launch {
            RetrofitClient.webService.getMovieDetails(movieId, apiKey, language)
                .enqueue(object : Callback<MovieDetailsResponse> {
                    override fun onResponse(call: Call<MovieDetailsResponse>, response: Response<MovieDetailsResponse>) {
                        if (response.isSuccessful) {
                            _movieDetails.value = response.body()
                        } else {
                            // Manejar error
                        }
                    }
                    override fun onFailure(call: Call<MovieDetailsResponse>, t: Throwable) {
                        // Manejar fallo
                    }
                })
        }
    }

    fun addToFavorites(onComplete: (Boolean) -> Unit) {
        currentMovieModel?.let { movie ->
            FirebaseClient.addToFavorites(movie, onComplete)
        } ?: onComplete(false)
    }

    fun removeFromFavorites(onComplete: (Boolean) -> Unit) {
        currentMovieModel?.let { movie ->
            FirebaseClient.removeFromFavorites(movie, onComplete)
        } ?: onComplete(false)
    }

    fun addToWatched(onComplete: (Boolean) -> Unit) {
        currentMovieModel?.let { movie ->
            FirebaseClient.addToWatched(movie, onComplete)
        } ?: onComplete(false)
    }

    fun removeFromWatched(onComplete: (Boolean) -> Unit) {
        currentMovieModel?.let { movie ->
            FirebaseClient.removeFromWatched(movie, onComplete)
        } ?: onComplete(false)
    }
}
