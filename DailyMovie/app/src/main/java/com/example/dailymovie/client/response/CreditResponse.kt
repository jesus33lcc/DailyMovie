package com.example.dailymovie.client.response

import com.example.dailymovie.models.CastMemberModel
import com.example.dailymovie.models.CrewMemberModel
import com.google.gson.annotations.SerializedName

data class CreditResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cast")
    val cast: List<CastMemberModel>,
    @SerializedName("crew")
    val crew: List<CrewMemberModel>
)
