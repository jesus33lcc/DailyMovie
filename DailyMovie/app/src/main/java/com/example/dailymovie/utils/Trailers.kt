package com.example.dailymovie.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
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

    /**
     * Abre la app de YouTube y, si no esta instalada, tira del navegador.
     *
     * Recibe la vista y no el contexto porque el aviso de que no se ha podido abrir sale
     * como Snackbar, que necesita una vista de la pantalla. Un Toast no valdria: desde
     * Android 12 no se pueden vestir y saldria con la caja gris del sistema.
     */
    fun abrir(vista: View, videoId: String) {
        val context = vista.context
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
                Avisos.breve(vista, context.getString(R.string.error_abrir_trailer))
            }
        }
    }
}
