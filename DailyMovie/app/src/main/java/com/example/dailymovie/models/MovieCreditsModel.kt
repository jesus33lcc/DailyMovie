package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

data class MovieCreditsModel(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cast")
    val cast: List<CastMemberModel>,
    @SerializedName("crew")
    val crew: List<CrewMemberModel>
)
