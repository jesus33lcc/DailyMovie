package com.example.dailymovie.utils

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.dailymovie.R
import com.google.android.material.snackbar.Snackbar

/**
 * Los avisos de abajo, con la cara de la app.
 *
 * El de borrar da unos segundos para arrepentirse en vez de preguntar antes con un dialogo.
 * Es el patron del correo: borras, ves lo que ha pasado y si te has equivocado lo deshaces.
 * Sale mas rapido que confirmar cada vez, y protege igual o mejor, porque el aviso aparece
 * cuando ya sabes que fila era.
 */
object Avisos {

    /**
     * Enseña el aviso y espera.
     *
     * Ojo con el orden: `alConfirmar` es donde va el borrado de verdad, y solo se llama si
     * el aviso se va solo. Quien llame a esto debe quitar la fila de la pantalla antes, para
     * que se vea desaparecer al instante.
     */
    fun conDeshacer(
        vista: View,
        texto: String,
        textoAccion: String = "Deshacer",
        alDeshacer: () -> Unit,
        alConfirmar: () -> Unit
    ) {
        val contexto = vista.context
        val aviso = Snackbar.make(vista, texto, Snackbar.LENGTH_LONG)

        aviso.setBackgroundTint(ContextCompat.getColor(contexto, R.color.colorAccent))
        aviso.setTextColor(ContextCompat.getColor(contexto, R.color.white))
        aviso.setActionTextColor(ContextCompat.getColor(contexto, R.color.colorPrimary))

        // El Snackbar no deja cambiar la fuente desde fuera, hay que buscar su TextView.
        aviso.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            ?.typeface = ResourcesCompat.getFont(contexto, R.font.courier)

        aviso.setAction(textoAccion) { alDeshacer() }
        aviso.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(aviso: Snackbar?, evento: Int) {
                // Si se fue porque el usuario pulso Deshacer, no hay nada que borrar.
                if (evento != DISMISS_EVENT_ACTION) alConfirmar()
            }
        })

        aviso.show()
    }
}
