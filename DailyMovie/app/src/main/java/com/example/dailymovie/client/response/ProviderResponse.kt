package com.example.dailymovie.client.response

import com.example.dailymovie.models.ProviderResultModel
import com.google.gson.annotations.SerializedName

data class ProviderResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: Map<String, ProviderResultModel>
)
