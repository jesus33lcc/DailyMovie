package com.example.dailymovie.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.response.MoviesResponse
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ExplorarViewModel : ViewModel() {
    private val _movies = MutableLiveData<List<MovieModel>>()
    val movies: LiveData<List<MovieModel>> = _movies
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    fun searchMovies(query: String){
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                val call = RetrofitClient.webService.searchMovies(query, Constantes.API_KEY)
                call.enqueue(object :Callback<MoviesResponse>{
                    override fun onResponse(
                        call: Call<MoviesResponse>,
                        response: Response<MoviesResponse>
                    ){
                        if(response.isSuccessful){
                            _movies.postValue(response.body()?.results)
                        }else{
                            _error.postValue("Error: ${response.code()}")
                        }
                    }

                    override fun onFailure(p0: Call<MoviesResponse>, p1: Throwable) {
                        _error.postValue(p1.message)
                    }
                })
            }
        }
    }
}