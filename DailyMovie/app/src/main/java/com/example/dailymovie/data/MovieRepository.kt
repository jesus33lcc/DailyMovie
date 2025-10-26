package com.example.dailymovie.data

import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.models.Hallazgo
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

    /**
     * Busca peliculas, series y gente de una vez.
     *
     * @param pagina cual de las tandas de 20 se pide. TMDB pagina de veinte en veinte y hasta
     *   ahora solo se pedia la primera, asi que cualquier busqueda tenia un techo de 20.
     */
    fun buscarTodo(consulta: String, pagina: Int = 1, alTerminar: (Resultado<Pagina>) -> Unit)

    /**
     * Las peliculas de una saga, en orden de estreno.
     *
     * @param sagaId el id de coleccion de TMDB.
     */
    fun peliculasDeLaSaga(sagaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /** Lo que esta en tendencia esta semana, mezclando peliculas y series. */
    fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit)

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
    fun porGeneros(
        generos: List<Int>,
        pagina: Int = 1,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    )

    fun generos(alTerminar: (Resultado<List<com.example.dailymovie.models.GenreModel>>) -> Unit)

    /**
     * Peliculas que cumplen unos filtros, sin que haga falta escribir nada.
     *
     * @param generos ids de genero, o vacio para no filtrar por genero.
     * @param filtros año, nota minima y plataforma. Los que vengan a null no se mandan.
     */
    fun descubrir(
        generos: List<Int> = emptyList(),
        filtros: FiltrosAvanzados = FiltrosAvanzados(),
        pagina: Int = 1,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    )

    /** Las plataformas de streaming del pais del aparato, para poder filtrar por ellas. */
    fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit)
}

/**
 * Una tanda de resultados y si detras hay mas.
 *
 * Hace falta para poder ir cargando segun se baja: sin saber si quedan paginas, o se pide de
 * mas a lo tonto o se deja de pedir antes de tiempo.
 */
data class Pagina(
    val hallazgos: List<Hallazgo>,
    val pagina: Int,
    val hayMas: Boolean
)

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
