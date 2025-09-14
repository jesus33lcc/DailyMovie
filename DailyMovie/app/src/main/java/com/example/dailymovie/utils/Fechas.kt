package com.example.dailymovie.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Las fechas de TMDB llegan siempre como "1972-03-14".
 *
 * Enseñarlas asi queda frio, y ademas el orden dia/mes cambia segun el pais, asi que se
 * pasan al formato largo del idioma del dispositivo.
 */
object Fechas {

    private const val FORMATO_TMDB = "yyyy-MM-dd"

    /** "1972-03-14" -> "14 de marzo de 1972". Si no se puede, devuelve lo que llego. */
    fun enLargo(fecha: String): String = try {
        val leida = SimpleDateFormat(FORMATO_TMDB, Locale.US).parse(fecha)
        if (leida == null) fecha
        else SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.getDefault()).format(leida)
    } catch (e: Exception) {
        fecha
    }

    /** Solo el año, que es lo que cabe al lado de un titulo. */
    fun soloElAno(fecha: String?): String = fecha?.take(4).orEmpty()
}
