package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.*
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga
import com.example.dailymovie.utils.LocaleUtil

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

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    fun setCurrentMovieModel(movieModel: MovieModel) {
        currentMovieModel = movieModel
    }

    fun fetchMovieDetails(movieId: Int, apiKey: String, language: String) {
        RetrofitClient.webService.getMovieDetails(movieId, apiKey, language).enqueueSimple(
            onExito = { _movieDetails.value = it },
            onError = { _error.value = it }
        )
    }

    fun fetchMovieProviders(movieId: Int, apiKey: String) {
        // Las secciones de abajo (plataformas, reparto, videos...) son un extra: si fallan
        // solo se ocultan, y no se avisa para no llenar la pantalla de mensajes. El aviso
        // se reserva para los datos principales de la pelicula.
        RetrofitClient.webService.getMovieProviders(movieId, apiKey).enqueueSimple(
            onExito = { _movieProviders.value = it },
            onError = { }
        )
    }

    fun fetchMovieCredits(movieId: Int, apiKey: String) {
        RetrofitClient.webService.getMovieCredits(movieId, apiKey).enqueueSimple(
            onExito = { _movieCredits.value = it },
            onError = { }
        )
    }

    fun fetchMovieVideos(movieId: Int, apiKey: String) {
        // Se piden dos veces: primero en el idioma del movil y luego en ingles, porque
        // TMDB tiene muchos mas trailers en ingles. Si la segunda falla nos quedamos con
        // los del idioma local en vez de dejar la seccion vacia.
        RetrofitClient.webService.getMovieVideos(movieId, apiKey, LocaleUtil.getLanguageAndCountry())
            .enqueueSimple(
                onExito = { respuestaLocal ->
                    val videosLocales = sortVideos(respuestaLocal.results)
                    RetrofitClient.webService.getMovieVideos(movieId, apiKey, "en-US").enqueueSimple(
                        onExito = { respuestaIngles ->
                            _movieVideos.value = videosLocales + sortVideos(respuestaIngles.results)
                        },
                        onError = { _movieVideos.value = videosLocales }
                    )
                },
                onError = { }
            )
    }

    private fun sortVideos(videos: List<VideoModel>): List<VideoModel> {
        val trailers = videos.filter { it.type == "Trailer" }
        val others = videos.filter { it.type != "Trailer" }
        return trailers + others
    }

    fun fetchSimilarMovies(movieId: Int, apiKey: String, language: String) {
        RetrofitClient.webService.getSimilarMovies(movieId, apiKey, language).enqueueSimple(
            onExito = { _similarMovies.value = it.results },
            onError = { }
        )
    }

    fun fetchRecommendedMovies(movieId: Int, apiKey: String, language: String) {
        RetrofitClient.webService.getRecommendedMovies(movieId, apiKey, language).enqueueSimple(
            onExito = { _recommendedMovies.value = it.results },
            onError = { }
        )
    }

    /** La vista avisa de que ya ha enseñado el aviso, para que no vuelva a salir solo. */
    fun errorMostrado() {
        _error.value = null
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
