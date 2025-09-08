package com.example.dailymovie.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.dailymovie.R

/**
 * Abre trailers de YouTube fuera de la app.
 *
 * Se llego aqui despues de comprobar que YouTube ya no deja reproducir sus videos dentro de
 * un WebView de Android: falla con "video no disponible" tanto con la libreria del
 * reproductor como cargando la URL a pelo o con un iframe, en dos dispositivos distintos y
 * con varios videos. Mientras no haya una via legitima para reproducir dentro, se enseña la
 * miniatura y el video se abre en YouTube.
 */
object Trailers {

    /** Miniatura oficial del video. La sirve YouTube sin necesidad de clave. */
    fun miniatura(videoId: String) = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    /** Abre la app de YouTube y, si no esta instalada, tira del navegador. */
    fun abrir(context: Context, videoId: String) {
        val enLaApp = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val enElNavegador = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("${Constantes.YOUTUBE_URL}$videoId")
        )

        try {
            context.startActivity(enLaApp)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(enElNavegador)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_abrir_trailer),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
