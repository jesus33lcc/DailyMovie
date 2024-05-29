package com.example.dailymovie.models

data class MovieOfTheDay(
    val title: String,
    val rating: Double,
    val review: String,
    val date: String,
    val author: String,
    val posterPath: String
)
