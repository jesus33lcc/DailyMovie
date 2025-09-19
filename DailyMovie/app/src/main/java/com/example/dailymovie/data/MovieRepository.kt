package com.example.dailymovie.data

import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga

/**
 * Todo lo que la app le pide a TMDB.
 *
 * Es una interfaz a proposito: antes los ViewModels llamaban directamente al object
 * RetrofitClient, asi que no habia forma de probarlos sin salir a internet. Contra esta
 * interfaz se puede poner un doble en los tests.
 */
interface MovieRepository {

    fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit)
    fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit)
    fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit)
    fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit)
    fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit)
    fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit)
    fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit)
    fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)
    fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /** Peliculas de un genero, ordenadas por popularidad. Alimenta la recomendacion. */
    fun porGeneros(generos: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    fun generos(alTerminar: (Resultado<List<com.example.dailymovie.models.GenreModel>>) -> Unit)
}

/**
 * Lo que devuelve el repositorio: o los datos, o el motivo por el que no hay datos.
 *
 * Evita el lio anterior de tener que mirar por un lado la lista y por otro un LiveData de
 * error para saber si algo habia ido bien.
 */
sealed class Resultado<out T : Any> {
    // El "T : Any" no es adorno: sin cota, el generico admite nulos y "datos" podria venir
    // vacio sin que nadie se entere. Con la cota, si algo puede faltar hay que decirlo.
    data class Exito<T : Any>(val datos: T) : Resultado<T>()
    data class Fallo(val motivo: ErrorCarga) : Resultado<Nothing>()
}

/** Atajo para no repetir el when cuando solo interesa el caso bueno. */
inline fun <T : Any> Resultado<T>.siVaBien(bloque: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Exito) bloque(datos)
    return this
}

/** Atajo para el caso malo. */
inline fun <T : Any> Resultado<T>.siFalla(bloque: (ErrorCarga) -> Unit): Resultado<T> {
    if (this is Resultado.Fallo) bloque(motivo)
    return this
}
