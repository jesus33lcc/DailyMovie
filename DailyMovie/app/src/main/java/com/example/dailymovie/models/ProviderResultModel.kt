package com.example.dailymovie.models

import com.google.gson.annotations.SerializedName

data class ProviderResultModel(
    @SerializedName("link")
    val link: String,
    @SerializedName("flatrate")
    val flatrate: List<ProviderDetailModel>?,
    @SerializedName("rent")
    val rent: List<ProviderDetailModel>?,
    @SerializedName("buy")
    val buy: List<ProviderDetailModel>?
)
