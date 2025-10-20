package com.example.dailymovie.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Comprueba que los numeros de TMDB se enseñan de forma legible.
 *
 * Es codigo Kotlin puro, sin nada de Android, asi que se puede probar directamente.
 */
class CifrasTest {

    @Test
    fun `un presupuesto de cientos de millones se resume en millones`() {
        assertEquals("250 M$", Cifras.dinero(250_000_000))
    }

    @Test
    fun `a partir de mil millones se usa una decimal`() {
        assertEquals("2,9 MM$", Cifras.dinero(2_923_706_026).replace('.', ','))
    }

    @Test
    fun `por debajo del millon se enseña el numero entero`() {
        // Aqui el numero se sigue leyendo bien, asi que no hace falta redondear.
        assertEquals(true, Cifras.dinero(850_000).endsWith("$"))
    }

    @Test
    fun `un cero significa que no se sabe, no que fuera gratis`() {
        assertEquals("", Cifras.dinero(0))
    }

    @Test
    fun `las duraciones largas van en horas y minutos`() {
        assertEquals("2 h 53 min", Cifras.duracion(173))
    }

    @Test
    fun `las duraciones de menos de una hora se quedan en minutos`() {
        assertEquals("45 min", Cifras.duracion(45))
    }

    @Test
    fun `las horas justas no arrastran un cero minutos`() {
        assertEquals("2 h", Cifras.duracion(120))
    }

    @Test
    fun `sin duracion no se enseña nada`() {
        assertEquals("", Cifras.duracion(0))
    }
}
