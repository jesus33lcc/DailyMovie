package com.example.dailymovie.utils

import android.view.View
import android.view.ViewGroup
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
     * Un aviso corto, de los de "hecho" o "no se ha podido".
     *
     * Sustituye a los Toast. No es capricho: desde Android 12 el sistema **ignora** los Toast
     * personalizados (setView esta desactivado por seguridad, para que ninguna app pueda
     * imitar avisos del sistema), asi que un Toast siempre sale con la caja gris de Android
     * y no hay forma de darle los colores ni la fuente de la app. Con la app en minSdk 33,
     * eso significa que un Toast nunca se va a parecer al resto.
     *
     * El Snackbar si se puede vestir, sale en el mismo sitio y ademas no tapa nada.
     *
     * @param vista cualquiera de la pantalla: de ella cuelga el aviso y por ella se llega a
     *   la barra de abajo para no taparla.
     * @param texto lo que se lee. Cabe en tres lineas.
     */
    fun breve(vista: View, texto: String) {
        vestir(Snackbar.make(vista, texto, Snackbar.LENGTH_SHORT), vista).show()
    }

    /**
     * Igual pero para textos que hay que leer con calma.
     *
     * @param vista cualquiera de la pantalla.
     * @param texto lo que se lee.
     */
    fun largo(vista: View, texto: String) {
        vestir(Snackbar.make(vista, texto, Snackbar.LENGTH_LONG), vista).show()
    }

    /**
     * @param origen la vista desde la que se pidio el aviso. Hace falta para buscar la barra
     *   de abajo: la vista del propio Snackbar todavia no esta metida en la pantalla cuando
     *   se le da forma, asi que desde ella no se encuentra nada.
     */
    private fun vestir(aviso: Snackbar, origen: View): Snackbar {
        val contexto = aviso.view.context

        // Si la pantalla tiene la barra de abajo, el aviso sube por encima en vez de taparla:
        // si no, se come los botones de navegar justo cuando uno podria querer irse.
        //
        // Se hace con margen y no con anchorView porque el anclaje de Material necesita un
        // CoordinatorLayout, y las pantallas de la app no lo usan: se probó y el aviso seguía
        // saliendo encima de la barra.
        origen.rootView.findViewById<View>(R.id.nav_view)
            ?.takeIf { it.visibility == View.VISIBLE && it.height > 0 }
            ?.let { barra ->
                (aviso.view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { medidas ->
                    medidas.bottomMargin += barra.height
                    aviso.view.layoutParams = medidas
                }
            }

        aviso.setBackgroundTint(ContextCompat.getColor(contexto, R.color.colorAccent))
        aviso.setTextColor(ContextCompat.getColor(contexto, R.color.white))
        aviso.setActionTextColor(ContextCompat.getColor(contexto, R.color.colorPrimary))
        aviso.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
            typeface = ResourcesCompat.getFont(contexto, R.font.courier_prime)
            // Los avisos de la app pueden ser largos ("Ya tienes una lista con ese nombre"),
            // y por defecto el Snackbar los corta en una linea.
            maxLines = 3
        }
        return aviso
    }

    /**
     * Enseña el aviso y espera.
     *
     * Ojo con el orden: `alConfirmar` es donde va el borrado de verdad, y solo se llama si
     * el aviso se va solo. Quien llame a esto debe quitar la fila de la pantalla antes, para
     * que se vea desaparecer al instante.
     *
     * @param vista cualquiera de la pantalla donde sale el aviso.
     * @param texto lo que acaba de pasar, para que se vea que fila era.
     * @param textoAccion el boton de echarse atras.
     * @param alDeshacer se llama al pulsar ese boton: aqui se devuelve la fila a su sitio.
     * @param alConfirmar se llama cuando el aviso se va solo, y es donde va el borrado de
     *   verdad. Si el usuario deshizo, no se llama.
     */
    fun conDeshacer(
        vista: View,
        texto: String,
        textoAccion: String = vista.context.getString(R.string.boton_deshacer),
        alDeshacer: () -> Unit,
        alConfirmar: () -> Unit
    ) {
        val aviso = vestir(Snackbar.make(vista, texto, Snackbar.LENGTH_LONG), vista)
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
