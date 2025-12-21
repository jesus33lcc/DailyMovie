package com.example.dailymovie.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailymovie.activities.viewmodels.Marcado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * El botón de guardar, que es el que se rompía al pulsarlo rápido.
 *
 * Aquí se puede probar de verdad porque [Marcado] no sabe nada de Firestore: recibe cómo
 * guardar y ya. El doble de guardado no contesta al momento, sino cuando el test lo dice, que
 * es justo lo que hace falta para reproducir el problema: dos toques mientras la primera
 * escritura sigue en el aire.
 */
class MarcadoTest {

    @get:Rule
    val reglaDeHiloPrincipal = InstantTaskExecutorRule()

    /**
     * Un guardado que se queda a medias hasta que el test lo suelta.
     *
     * Guarda lo que le han pedido y con qué orden, para poder mirar después cuántas veces se
     * escribió de verdad.
     */
    private class GuardadoFalso {
        val pedidos = mutableListOf<Boolean>()
        private val pendientes = mutableListOf<(Boolean) -> Unit>()

        /** Se le pasa a Marcado como su forma de guardar. */
        fun comoGuardar(): (Boolean, (Boolean) -> Unit) -> Unit = { comoQuedar, alTerminar ->
            pedidos += comoQuedar
            pendientes += alTerminar
        }

        /** Contesta a la escritura que está esperando. */
        fun contestar(bien: Boolean = true) {
            pendientes.removeAt(0).invoke(bien)
        }

        fun hayAlgoEnElAire() = pendientes.isNotEmpty()
    }

    @Test
    fun `el boton cambia al momento, sin esperar a que se guarde`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        marcado.cambiar()

        // Todavia no ha contestado nadie y el corazon ya esta puesto.
        assertEquals(true, marcado.puesto.value)
        assertTrue(guardado.hayAlgoEnElAire())
    }

    @Test
    fun `dos toques seguidos no lanzan dos escrituras a la vez`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        marcado.cambiar()
        marcado.cambiar()

        // El segundo toque no escribe: espera a que termine el primero. Este era el fallo de
        // antes, donde los dos leian "no esta" y los dos metian la pelicula.
        assertEquals(listOf(true), guardado.pedidos)
        assertEquals(false, marcado.puesto.value)
    }

    @Test
    fun `al acabar la escritura se guarda lo ultimo que pidio el usuario`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        marcado.cambiar()      // quiere true, empieza a guardar
        marcado.cambiar()      // cambia de idea: quiere false
        guardado.contestar()   // termina la escritura del true

        // Se da cuenta de que ya no vale y vuelve a escribir con lo bueno.
        assertEquals(listOf(true, false), guardado.pedidos)
    }

    @Test
    fun `aporrear el boton no lanza una escritura por toque`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        repeat(9) { marcado.cambiar() }
        guardado.contestar()

        // Nueve toques desde apagado acaban en encendido, que es justo lo que se mando a
        // guardar con el primero: una sola escritura para nueve toques.
        assertEquals(listOf(true), guardado.pedidos)
        assertEquals(true, marcado.puesto.value)
    }

    @Test
    fun `un numero par de toques se queda como estaba y no escribe de mas`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        marcado.cambiar()
        marcado.cambiar()
        guardado.contestar()

        // Acaba donde empezo, asi que la segunda escritura devuelve el false original.
        assertEquals(listOf(true, false), guardado.pedidos)
        assertEquals(false, marcado.puesto.value)
    }

    @Test
    fun `si no se puede guardar, el boton vuelve a donde estaba`() {
        val guardado = GuardadoFalso()
        var avisado = false
        val marcado = Marcado(guardado.comoGuardar())
        marcado.empezarEn(false)

        marcado.cambiar { avisado = true }
        guardado.contestar(bien = false)

        assertEquals(false, marcado.puesto.value)
        assertTrue("hay que avisar de que no se ha guardado", avisado)
    }

    @Test
    fun `abrir una ficha ya guardada no escribe nada`() {
        val guardado = GuardadoFalso()
        val marcado = Marcado(guardado.comoGuardar())

        marcado.empezarEn(true)

        assertEquals(true, marcado.puesto.value)
        assertEquals(emptyList<Boolean>(), guardado.pedidos)
    }
}
