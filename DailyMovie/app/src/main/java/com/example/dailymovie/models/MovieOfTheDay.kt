package com.example.dailymovie.models

data class MovieOfTheDay(
    val id: Int,
    val title: String,
    val review: String,
    val date: String,
    val author: String,
    val videoId: String
)
