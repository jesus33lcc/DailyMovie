package com.example.dailymovie.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Las fechas de TMDB llegan siempre como "1972-03-14".
 *
 * Enseñarlas asi queda frio, y ademas el orden dia/mes cambia segun el pais, asi que se
 * pasan al formato largo del idioma del dispositivo.
 */
object Fechas {

    private const val FORMATO_TMDB = "yyyy-MM-dd"

    /**
     * "1972-03-14" -> "14 de marzo de 1972". Si no se puede, devuelve lo que llego.
     *
     * @param fecha tal y como la manda TMDB. Tambien vale una cadena vacia, que es lo que
     *   pasa con lo que aun no tiene fecha de estreno.
     */
    fun enLargo(fecha: String): String = try {
        val leida = SimpleDateFormat(FORMATO_TMDB, Locale.US).parse(fecha)
        if (leida == null) fecha
        else SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.getDefault()).format(leida)
    } catch (e: Exception) {
        fecha
    }

    /**
     * Solo el año, que es lo que cabe al lado de un titulo.
     *
     * Se cortan los cuatro primeros caracteres en vez de leer la fecha: es mas rapido y da
     * igual que venga incompleta o rara, porque el año siempre va delante.
     *
     * @param fecha la de TMDB, o null si no la hay.
     * @return "1972", o cadena vacia si no habia fecha.
     */
    fun soloElAno(fecha: String?): String = fecha?.take(4).orEmpty()

    /**
     * Si esa fecha todavia no ha llegado.
     *
     * TMDB da igual de completos los episodios ya emitidos que los que estan anunciados, asi
     * que sin esto una serie en emision enseña los que quedan como si se pudieran ver ya.
     *
     * Se compara contra el principio del dia de hoy: lo que se estrena hoy cuenta como
     * estrenado, aunque sea por la noche.
     *
     * @param fecha la de TMDB. Si viene a null, vacia o mal escrita se responde que no, para
     *   no acabar avisando de "proximamente" sobre algo que se estreno hace veinte años.
     */
    fun estaPorVenir(fecha: String?): Boolean {
        val texto = fecha?.takeIf { it.isNotBlank() } ?: return false
        return try {
            val leida = SimpleDateFormat(FORMATO_TMDB, Locale.US).parse(texto) ?: return false
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            leida.after(hoy.time)
        } catch (e: Exception) {
            false
        }
    }
}
