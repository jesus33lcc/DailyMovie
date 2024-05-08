package com.example.dailymovie.client

import com.example.dailymovie.client.response.MoviesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WebService {
    @GET("search/movie")
    fun searchMovies(
        @Query("query") title: String,
        @Query("api_key") apiKey: String,
        @Query("include_adult") include_adult: Boolean = true,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>
}