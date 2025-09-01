package com.example.dailymovie.client

import com.example.dailymovie.utils.ErrorCarga
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * Comprueba que `enqueueSimple` reparte bien cada situacion.
 *
 * Es justo lo que fallaba antes: los `onFailure` estaban vacios, asi que un corte de red
 * no llegaba nunca a la pantalla. Aqui se fuerza cada caso sin tocar internet.
 */
class RetrofitExtensionsTest {

    /** Call de mentira: no llama a la red, contesta lo que se le diga al construirla. */
    private class CallFalsa<T>(
        private val respuesta: Response<T>? = null,
        private val fallo: Throwable? = null
    ) : Call<T> {

        override fun enqueue(callback: Callback<T>) {
            respuesta?.let { callback.onResponse(this, it) }
            fallo?.let { callback.onFailure(this, it) }
        }

        override fun execute(): Response<T> = throw UnsupportedOperationException()
        override fun clone(): Call<T> = this
        override fun isExecuted(): Boolean = false
        override fun cancel() {}
        override fun isCanceled(): Boolean = false
        override fun request(): Request = Request.Builder().url("https://test.local/").build()
        override fun timeout(): Timeout = Timeout.NONE
    }

    private fun cuerpoDeError(): ResponseBody =
        ResponseBody.create(MediaType.parse("text/plain"), "")

    @Test
    fun `si la respuesta es correcta entrega los datos y no da error`() {
        var recibido: String? = null
        var error: ErrorCarga? = null

        CallFalsa(Response.success("peliculas")).enqueueSimple(
            onExito = { recibido = it },
            onError = { error = it }
        )

        assertEquals("peliculas", recibido)
        assertNull(error)
    }

    @Test
    fun `si el servidor contesta con un codigo de error avisa de respuesta invalida`() {
        var recibido: String? = null
        var error: ErrorCarga? = null

        CallFalsa<String>(Response.error(500, cuerpoDeError())).enqueueSimple(
            onExito = { recibido = it },
            onError = { error = it }
        )

        assertNull(recibido)
        assertEquals(ErrorCarga.RESPUESTA_INVALIDA, error)
    }

    @Test
    fun `si la respuesta llega vacia avisa de respuesta invalida`() {
        var recibido: String? = null
        var error: ErrorCarga? = null

        CallFalsa(Response.success<String>(null)).enqueueSimple(
            onExito = { recibido = it },
            onError = { error = it }
        )

        assertNull(recibido)
        assertEquals(ErrorCarga.RESPUESTA_INVALIDA, error)
    }

    @Test
    fun `si no hay red avisa de falta de conexion`() {
        var recibido: String? = null
        var error: ErrorCarga? = null

        CallFalsa<String>(fallo = IOException("sin red")).enqueueSimple(
            onExito = { recibido = it },
            onError = { error = it }
        )

        assertNull(recibido)
        assertEquals(ErrorCarga.SIN_CONEXION, error)
    }
}
