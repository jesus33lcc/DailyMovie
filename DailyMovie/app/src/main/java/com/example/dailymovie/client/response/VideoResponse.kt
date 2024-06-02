package com.example.dailymovie.client.response

import com.example.dailymovie.models.VideoModel
import com.google.gson.annotations.SerializedName

data class VideoResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("results")
    val results: List<VideoModel>
)
