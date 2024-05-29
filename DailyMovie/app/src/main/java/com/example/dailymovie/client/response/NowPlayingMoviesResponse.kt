package com.example.dailymovie.client.response

import com.example.dailymovie.models.MovieModel
import com.google.gson.annotations.SerializedName

data class NowPlayingMoviesResponse(
    @SerializedName("results")
    var results: List<MovieModel>
)
