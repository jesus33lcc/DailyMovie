package com.example.dailymovie.client

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * El interceptor que le pone la clave y el idioma a cada petición.
 *
 * Se prueba porque es un único punto del que depende **todo**: si deja de añadir la clave, no
 * carga nada en ninguna pantalla, y eso antes no podía pasar porque cada endpoint la llevaba
 * escrita. Al centralizarlo hay que asegurarse de que lo hace bien.
 *
 * No hace falta MockWebServer: se le pasa una cadena falsa que se queda con la petición ya
 * modificada y devuelve una respuesta vacía.
 */
class ClaveEIdiomaTest {

    @Test
    fun `a toda peticion se le pone la clave`() {
        val salida = pasarPor("https://api.themoviedb.org/3/movie/popular")

        assertNotNull("falta la clave", salida.url().queryParameter("api_key"))
    }

    @Test
    fun `si no trae idioma se le pone el del aparato`() {
        val salida = pasarPor("https://api.themoviedb.org/3/movie/popular")

        assertNotNull("falta el idioma", salida.url().queryParameter("language"))
    }

    @Test
    fun `si la peticion ya trae idioma no se le pone otro`() {
        // Los videos se piden tambien en ingles a proposito: muchos trailers solo estan ahi.
        // Si el interceptor añadiera el suyo, la peticion iria con dos "language" y TMDB
        // elegiria uno de los dos sin decir cual.
        val salida = pasarPor("https://api.themoviedb.org/3/movie/550/videos?language=en-US")

        assertEquals(1, salida.url().queryParameterValues("language").size)
        assertEquals("en-US", salida.url().queryParameter("language"))
    }

    @Test
    fun `lo que ya llevaba la peticion se respeta`() {
        val salida = pasarPor("https://api.themoviedb.org/3/search/multi?query=matrix&page=2")

        assertEquals("matrix", salida.url().queryParameter("query"))
        assertEquals("2", salida.url().queryParameter("page"))
    }

    /** Mete la direccion por el interceptor y devuelve la peticion tal y como sale. */
    private fun pasarPor(direccion: String): Request {
        var loQueSalio: Request? = null
        val cadena = CadenaFalsa(Request.Builder().url(direccion).build()) { loQueSalio = it }
        ClaveEIdioma().intercept(cadena)
        return loQueSalio!!
    }

    /**
     * Una cadena de OkHttp que no llama a nadie.
     *
     * Solo se queda con la peticion que le pasa el interceptor y devuelve una respuesta
     * vacia, que es lo unico que hace falta para mirar como quedo la direccion.
     */
    private class CadenaFalsa(
        private val peticion: Request,
        private val alRecibir: (Request) -> Unit
    ) : Interceptor.Chain {

        override fun request() = peticion

        override fun proceed(request: Request): Response {
            alRecibir(request)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
        }

        // El resto de la interfaz no la usa el interceptor.
        override fun connection() = null
        override fun call() = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    }
}
