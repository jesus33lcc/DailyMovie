package com.example.dailymovie.graphics

import android.view.View
import android.widget.TextView

/**
 * Enseña u oculta una vista.
 *
 * Es lo mismo que `visibility = if (x) VISIBLE else GONE`, pero eso estaba escrito 56 veces
 * por toda la app y en las fichas iba de dos en dos (la etiqueta y su valor), asi que las
 * funciones de pintar se llenaban de ruido y costaba ver que se estaba enseñando de verdad.
 *
 * Se usa GONE y no INVISIBLE a proposito: en estas pantallas, lo que no hay tiene que dejar
 * de ocupar sitio, o quedan huecos en mitad de la ficha.
 *
 * @param condicion si tiene que verse.
 */
fun View.mostrarSi(condicion: Boolean) {
    visibility = if (condicion) View.VISIBLE else View.GONE
}

/**
 * Pone el texto, y si no hay nada que poner esconde la vista.
 *
 * En una ficha de TMDB casi cualquier campo puede llegar vacio: una pelicula sin presupuesto,
 * una serie sin sinopsis, alguien sin fecha de nacimiento. La pareja "pon el texto y esconde
 * si esta vacio" estaba repetida en cada uno.
 *
 * @param texto lo que se enseña. Si es null o esta en blanco, la vista se esconde.
 */
fun TextView.ponerOEsconder(texto: String?) {
    text = texto.orEmpty()
    mostrarSi(!texto.isNullOrBlank())
}

/**
 * Lo mismo que [ponerOEsconder] pero escondiendo tambien la etiqueta de al lado.
 *
 * En la ficha de pelicula cada dato son dos vistas: "Presupuesto:" y el valor. Si no hay
 * valor, dejar la etiqueta suelta queda peor que no enseñar nada.
 *
 * @param texto lo que se enseña.
 * @param etiqueta el "Presupuesto:" de al lado.
 */
fun TextView.ponerOEsconder(texto: String?, etiqueta: View) {
    ponerOEsconder(texto)
    etiqueta.mostrarSi(!texto.isNullOrBlank())
}
