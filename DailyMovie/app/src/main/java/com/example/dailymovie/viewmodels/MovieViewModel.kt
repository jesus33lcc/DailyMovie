package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.response.*
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieViewModel : ViewModel() {

    private var currentMovieModel: MovieModel? = null
    private val _movieDetails = MutableLiveData<MovieDetailsResponse>()
    val movieDetails: LiveData<MovieDetailsResponse> get() = _movieDetails
    private val _movieProviders = MutableLiveData<ProviderResponse>()
    val movieProviders: LiveData<ProviderResponse> get() = _movieProviders
    private val _movieCredits = MutableLiveData<CreditResponse>()
    val movieCredits: LiveData<CreditResponse> get() = _movieCredits
    private val _movieVideos = MutableLiveData<List<VideoModel>>()
    val movieVideos: LiveData<List<VideoModel>> get() = _movieVideos
    private val _similarMovies = MutableLiveData<List<MovieModel>>()
    val similarMovies: LiveData<List<MovieModel>> get() = _similarMovies
    private val _recommendedMovies = MutableLiveData<List<MovieModel>>()
    val recommendedMovies: LiveData<List<MovieModel>> get() = _recommendedMovies
    private val _favorites = MutableLiveData<List<MovieModel>>()
    val favorites: LiveData<List<MovieModel>> get() = _favorites

    private val _watched = MutableLiveData<List<MovieModel>>()
    val watched: LiveData<List<MovieModel>> get() = _watched
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

    fun fetchMovieProviders(movieId: Int, apiKey: String) {
        viewModelScope.launch {
            RetrofitClient.webService.getMovieProviders(movieId, apiKey)
                .enqueue(object : Callback<ProviderResponse> {
                    override fun onResponse(call: Call<ProviderResponse>, response: Response<ProviderResponse>) {
                        if (response.isSuccessful) {
                            _movieProviders.value = response.body()
                        } else {
                            // Manejar error
                        }
                    }

                    override fun onFailure(call: Call<ProviderResponse>, t: Throwable) {
                        // Manejar fallo
                    }
                })
        }
    }

    fun fetchMovieCredits(movieId: Int, apiKey: String) {
        viewModelScope.launch {
            RetrofitClient.webService.getMovieCredits(movieId, apiKey)
                .enqueue(object : Callback<CreditResponse> {
                    override fun onResponse(call: Call<CreditResponse>, response: Response<CreditResponse>) {
                        if (response.isSuccessful) {
                            _movieCredits.value = response.body()
                        } else {
                            // Manejar error
                        }
                    }

                    override fun onFailure(call: Call<CreditResponse>, t: Throwable) {
                        // Manejar fallo
                    }
                })
        }
    }

    fun fetchMovieVideos(movieId: Int, apiKey: String) {
        RetrofitClient.webService.getMovieVideos(movieId, apiKey, "es").enqueue(object : Callback<VideoResponse> {
            override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                val spanishVideos = response.body()?.results ?: emptyList()
                val sortedSpanishVideos = sortVideos(spanishVideos)

                RetrofitClient.webService.getMovieVideos(movieId, apiKey, "en-US").enqueue(object : Callback<VideoResponse> {
                    override fun onResponse(call: Call<VideoResponse>, response: Response<VideoResponse>) {
                        val englishVideos = response.body()?.results ?: emptyList()
                        val sortedEnglishVideos = sortVideos(englishVideos)
                        val combinedVideos = sortedSpanishVideos + sortedEnglishVideos

                        _movieVideos.postValue(combinedVideos)
                    }

                    override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                        _movieVideos.postValue(sortedSpanishVideos)  // Show Spanish videos even if English request fails
                    }
                })
            }

            override fun onFailure(call: Call<VideoResponse>, t: Throwable) {
                // Handle failure for Spanish videos
            }
        })
    }

    private fun sortVideos(videos: List<VideoModel>): List<VideoModel> {
        val trailers = videos.filter { it.type == "Trailer" }
        val others = videos.filter { it.type != "Trailer" }
        return trailers + others
    }

    fun fetchSimilarMovies(movieId: Int, apiKey: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getSimilarMovies(movieId, apiKey, language).enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _similarMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }

                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun fetchRecommendedMovies(movieId: Int, apiKey: String, language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getRecommendedMovies(movieId, apiKey, language).enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _recommendedMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }

                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun toggleFavorite(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        FirebaseClient.isMovieInFavorites(movie.id) { isFavorite ->
            if (isFavorite) {
                FirebaseClient.removeFromFavorites(movie) { success ->
                    onComplete(success)
                }
            } else {
                FirebaseClient.addToFavorites(movie) { success ->
                    onComplete(success)
                }
            }
        }
    }

    fun toggleWatched(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        FirebaseClient.isMovieInWatched(movie.id) { isWatched ->
            if (isWatched) {
                FirebaseClient.removeFromWatched(movie) { success ->
                    onComplete(success)
                }
            } else {
                FirebaseClient.addToWatched(movie) { success ->
                    onComplete(success)
                }
            }
        }
    }

    fun getCustomLists(onComplete: (List<String>) -> Unit) {
        FirebaseClient.getCustomLists { listNames ->
            onComplete(listNames)
        }
    }

    fun addMovieToList(listName: String, movie: MovieModel, onComplete: (Boolean) -> Unit) {
        FirebaseClient.addMovieToList(listName, movie) { success ->
            onComplete(success)
        }
    }

    fun removeMovieFromList(listName: String, movie: MovieModel, onComplete: (Boolean) -> Unit) {
        FirebaseClient.removeMovieFromList(listName, movie) { success ->
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
}
