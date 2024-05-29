package com.example.dailymovie.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.response.MoviesResponse
import com.example.dailymovie.client.response.NowPlayingMoviesResponse
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {

    private val _nowPlayingMovies = MutableLiveData<List<MovieModel>>()
    val nowPlayingMovies: LiveData<List<MovieModel>> get() = _nowPlayingMovies
    private val _popularMovies = MutableLiveData<List<MovieModel>>()
    val popularMovies: LiveData<List<MovieModel>> get() = _popularMovies
    private val _topRatedMovies = MutableLiveData<List<MovieModel>>()
    val topRatedMovies: LiveData<List<MovieModel>> get() = _topRatedMovies
    private val _upcomingMovies = MutableLiveData<List<MovieModel>>()
    val upcomingMovies: LiveData<List<MovieModel>> get() = _upcomingMovies
    private val _movieOfTheDay = MutableLiveData<MovieOfTheDay>()
    val movieOfTheDay: LiveData<MovieOfTheDay> get() = _movieOfTheDay
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchNowPlayingMovies(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getNowPlayingMovies(apiKey).enqueue(object :
                Callback<MoviesResponse> {
                override fun onResponse(
                    call: Call<MoviesResponse>,
                    response: Response<MoviesResponse>
                ) {
                    if (response.isSuccessful) {
                        _nowPlayingMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }

                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun fetchPopularMovies(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getPopularMovies(apiKey).enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _popularMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun fetchTopRatedMovies(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getTopRatedMovies(apiKey).enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _topRatedMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun fetchUpcomingMovies(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RetrofitClient.webService.getUpcomingMovies(apiKey).enqueue(object : Callback<MoviesResponse> {
                override fun onResponse(call: Call<MoviesResponse>, response: Response<MoviesResponse>) {
                    if (response.isSuccessful) {
                        _upcomingMovies.postValue(response.body()?.results ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<MoviesResponse>, t: Throwable) {
                    // Handle failure
                }
            })
        }
    }

    fun fetchMovieOfTheDay() {
        viewModelScope.launch(Dispatchers.IO) {
            FirebaseClient.getMovieOfTheDay { movie ->
                _movieOfTheDay.postValue(movie)
            }
        }
    }
}