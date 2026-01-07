package com.example.dailymovie.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.example.dailymovie.R

/**
 * Lo que lleva una ficha fuera de la app: los enlaces y el compartir.
 *
 * Los tres estaban escritos en cada pantalla: los enlaces dos veces (pelicula y serie),
 * compartir tres (pelicula, serie y lista) y abrir el navegador dos. Son unas cien lineas
 * que hacian exactamente lo mismo, y el aviso de "no hay navegador" solo estaba en dos de
 * los tres sitios.
 */

/**
 * El menu de "ver en otras webs".
 *
 * TMDB da el id de IMDb y la web oficial en la misma peticion de los detalles, asi que no
 * cuesta nada ofrecerlos: es donde la gente va a mirar la ficha de verdad o a comprar la
 * entrada. Los que no tenga la ficha simplemente no salen.
 *
 * @param boton el que se ha pulsado; el menu se abre pegado a el.
 * @param imdbId el id de IMDb ("tt0133093"), si lo hay.
 * @param webOficial la direccion de la web de la pelicula o serie, si la tiene.
 * @param enTmdb la direccion de la ficha en TMDB, que siempre existe.
 */
fun AppCompatActivity.menuDeEnlaces(
    boton: View,
    imdbId: String?,
    webOficial: String?,
    enTmdb: String
) {
    val enlaces = buildList {
        imdbId?.takeIf { it.isNotBlank() }
            ?.let { add("Ver en IMDb" to "https://www.imdb.com/title/$it/") }
        webOficial?.takeIf { it.isNotBlank() }?.let { add("Web oficial" to it) }
        add("Ver en TMDB" to enTmdb)
    }

    PopupMenu(ContextThemeWrapper(this, R.style.TemaPopupDailyMovie), boton).apply {
        enlaces.forEachIndexed { posicion, (titulo, _) -> menu.add(0, posicion, posicion, titulo) }
        setOnMenuItemClickListener { opcion ->
            abrirEnElNavegador(enlaces[opcion.itemId].second)
            true
        }
        show()
    }
}

/**
 * Abre una direccion fuera de la app.
 *
 * Puede no haber navegador (una tablet muy pelada, un perfil restringido), asi que se avisa
 * en vez de dejar que reviente.
 *
 * @param direccion a donde se va.
 * @param dondeAvisar una vista de la pantalla, para poder enseñar el aviso si no se puede.
 */
fun Activity.abrirEnElNavegador(direccion: String, dondeAvisar: View? = null) {
    val intento = Intent(Intent.ACTION_VIEW, Uri.parse(direccion))
    if (intento.resolveActivity(packageManager) != null) {
        startActivity(intento)
    } else {
        dondeAvisar?.let { Avisos.breve(it, "No hay ningún navegador para abrirlo") }
    }
}

/**
 * Abre el "compartir" de Android con una recomendacion.
 *
 * @param titulo el nombre de lo que se recomienda.
 * @param direccion el enlace que se manda, normalmente la ficha en TMDB.
 */
fun Activity.compartirRecomendacion(titulo: String, direccion: String) {
    val intento = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, "Te recomiendo esto: $titulo\n$direccion")
        type = "text/plain"
    }
    startActivity(Intent.createChooser(intento, "Compartir con"))
}
