package com.example.dailymovie.models

/**
 * La pelicula que se enseña al abrir la app.
 *
 * Puede venir de dos sitios y no son lo mismo, aunque acaben en la misma tarjeta:
 *  - **Curada a mano** en Firebase: alguien escribio una reseña y la firma.
 *  - **Recomendada** por tus gustos: no hay reseña ni autor, lo que hay es un motivo.
 *
 * Antes el motivo se metia en `author`, asi que la portada llegaba a poner
 * "Autor: Porque te gusta este tipo de cine". Son dos cosas distintas y ahora van aparte.
 */
data class MovieOfTheDay(
    val id: Int,
    val title: String,
    val review: String,
    val date: String,

    /** Quien firma la reseña. Solo en las curadas a mano. */
    val author: String? = null,

    /** Por que se te enseña esta. Solo en las recomendadas. */
    val motivo: String? = null,

    val videoId: String
)
