package com.example.dailymovie.utils

import android.content.Context

/**
 * Lo ultimo que se ha buscado, para poder repetirlo de un toque.
 *
 * No va a Firestore a proposito: son las palabras que escribes, no datos de la cuenta, y no
 * tiene sentido que viajen a otro movil ni que ocupen sitio en la base. Con las preferencias
 * del aparato sobra.
 *
 * Ojo: esto no es el historial. El historial guarda las fichas que has abierto y ese si va
 * en Firestore, porque es tuyo mires donde lo mires.
 */
object BusquedasRecientes {

    private const val FICHERO = "ajustes_dailymovie"
    private const val CLAVE = "busquedas_recientes"
    private const val SEPARADOR = "\n"
    private const val CUANTAS = 8

    fun guardar(context: Context, consulta: String) {
        val limpia = consulta.trim()
        if (limpia.length < 2) return

        // La ultima va primero y no se repite: si buscas lo mismo dos veces, sube arriba en
        // vez de aparecer dos veces.
        val nuevas = (listOf(limpia) + todas(context).filter { !it.equals(limpia, true) })
            .take(CUANTAS)

        preferencias(context).edit()
            .putString(CLAVE, nuevas.joinToString(SEPARADOR))
            .apply()
    }

    fun todas(context: Context): List<String> =
        preferencias(context).getString(CLAVE, "")
            ?.split(SEPARADOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()

    fun olvidar(context: Context, consulta: String) {
        val quedan = todas(context).filter { !it.equals(consulta, true) }
        preferencias(context).edit().putString(CLAVE, quedan.joinToString(SEPARADOR)).apply()
    }

    fun olvidarTodas(context: Context) {
        preferencias(context).edit().remove(CLAVE).apply()
    }

    private fun preferencias(context: Context) =
        context.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
}
