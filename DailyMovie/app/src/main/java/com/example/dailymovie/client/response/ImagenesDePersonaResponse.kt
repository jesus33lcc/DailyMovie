package com.example.dailymovie.client.response

import com.google.gson.annotations.SerializedName

/**
 * Lo que contesta person/{id}/images.
 *
 * TMDB las llama "profiles" y son retratos, casi siempre verticales. Vienen ordenadas por
 * votos, asi que las primeras suelen ser las mejores.
 */
data class ImagenesDePersonaResponse(
    @SerializedName("profiles")
    val fotos: List<FotoDePersona> = emptyList()
)

data class FotoDePersona(
    @SerializedName("file_path")
    val ruta: String
)
