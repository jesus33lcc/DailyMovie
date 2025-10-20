package com.example.dailymovie.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Números que vienen de TMDB y hay que enseñar a una persona.
 *
 * TMDB los da en crudo: el presupuesto de una película llega como `250000000` y la duración
 * como `173`. Puestos tal cual en la ficha no se leen, hay que contar los ceros.
 */
object Cifras {

    /**
     * Dinero, redondeado a lo que se entiende de un vistazo.
     *
     * Se pierde precisión a propósito: a nadie le importa si una película costó 250.000.000
     * o 249.800.000 dólares, lo que dice algo es "250 M$". Por debajo del millón sí se ponen
     * los separadores de miles, que ahí el número entero se sigue leyendo bien.
     *
     * @param cantidad importe en dólares, tal y como lo devuelve TMDB. Un 0 significa que no
     *   se sabe, no que fuera gratis.
     * @return el importe listo para pintar, o cadena vacía si no hay dato.
     */
    fun dinero(cantidad: Long): String = when {
        cantidad <= 0 -> ""
        cantidad >= 1_000_000_000 -> "%.1f MM$".format(Locale.getDefault(), cantidad / 1_000_000_000.0)
        cantidad >= 1_000_000 -> "${cantidad / 1_000_000} M$"
        else -> "${NumberFormat.getInstance(Locale.getDefault()).format(cantidad)} $"
    }

    /**
     * Minutos a horas y minutos.
     *
     * @param minutos duración en minutos. Un 0 o un negativo significan que TMDB no lo sabe.
     * @return "2 h 53 min", "45 min", o cadena vacía si no hay dato.
     */
    fun duracion(minutos: Int): String = when {
        minutos <= 0 -> ""
        minutos < 60 -> "$minutos min"
        minutos % 60 == 0 -> "${minutos / 60} h"
        else -> "${minutos / 60} h ${minutos % 60} min"
    }
}
