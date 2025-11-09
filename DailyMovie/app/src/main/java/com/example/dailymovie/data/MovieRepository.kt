package com.example.dailymovie.data

import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.ResenaResponse
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
 *
 * Todas las funciones avisan por callback y no devuelven nada: la respuesta llega despues, en
 * el hilo principal, envuelta en un [Resultado] que ya trae o los datos o el motivo del fallo.
 * Ninguna lanza excepciones, asi que el `alTerminar` se llama siempre, vaya bien o vaya mal.
 */
interface MovieRepository {

    /**
     * Busca peliculas por titulo.
     *
     * @param consulta lo que ha escrito el usuario.
     * @param alTerminar recibe las peliculas que encajan, o un fallo si no hubo red o TMDB
     *   contesto con un error. Que no haya coincidencias no es un fallo: llega una lista vacia.
     */
    fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Busca peliculas, series y gente de una vez.
     *
     * @param consulta lo que ha escrito el usuario.
     * @param pagina cual de las tandas de 20 se pide. TMDB pagina de veinte en veinte y hasta
     *   ahora solo se pedia la primera, asi que cualquier busqueda tenia un techo de 20.
     * @param alTerminar recibe la tanda y si detras hay mas. En la primera pagina se le cuelan
     *   delante las sagas que encajen, que van por otro endpoint; si ese endpoint falla no se
     *   pierde la busqueda, llegan igualmente los resultados normales.
     */
    fun buscarTodo(consulta: String, pagina: Int = 1, alTerminar: (Resultado<Pagina>) -> Unit)

    /**
     * Las peliculas de una saga, en orden de estreno.
     *
     * @param sagaId el id de coleccion de TMDB.
     * @param alTerminar recibe las peliculas de la saga, o un fallo si no se pudo pedir.
     */
    fun peliculasDeLaSaga(sagaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Lo que esta en tendencia esta semana, mezclando peliculas y series.
     *
     * @param alTerminar recibe lo que hay en tendencia, ya limpio de resultados a medias y con
     *   cada cosa marcada como lo que es, o un fallo si no se pudo pedir.
     */
    fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit)

    /**
     * Lo que hay ahora mismo en los cines.
     *
     * @param alTerminar recibe la cartelera, o un fallo si no hubo red o TMDB contesto mal.
     */
    fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Lo mas visto del momento.
     *
     * @param alTerminar recibe las populares, o un fallo si no se pudo pedir. Es la ultima
     *   parada de la portada, asi que si esto falla no hay nada mas que enseñar.
     */
    fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Las mejor valoradas de siempre segun los votos de TMDB.
     *
     * @param alTerminar recibe las peliculas, o un fallo si no se pudo pedir.
     */
    fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Lo que esta por estrenarse.
     *
     * @param alTerminar recibe los proximos estrenos, o un fallo si no se pudo pedir.
     */
    fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * La ficha completa de una pelicula.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe los detalles con la galeria, la clasificacion por edad y el id
     *   de IMDb dentro, que se piden en el mismo viaje. Si falla, llega el motivo.
     */
    fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit)

    /**
     * Donde se puede ver una pelicula.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe las plataformas de todos los paises a la vez; hay que quedarse
     *   con las del pais del aparato. Que ese pais no venga no es un fallo: es que ahi no esta.
     */
    fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit)

    /**
     * Quien sale y quien trabaja en una pelicula.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe el reparto y el equipo por separado. Al director hay que
     *   buscarlo en el equipo por su puesto, no viene señalado de otra forma.
     */
    fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit)

    /**
     * Los videos de una pelicula.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe los videos con los trailers delante, primero los del idioma del
     *   aparato y detras los de ingles, que es donde TMDB tiene casi todo. Solo llega un fallo
     *   si se cae la primera peticion: si solo falla la de ingles, se devuelve lo que haya.
     */
    fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit)

    /**
     * Peliculas parecidas, segun los generos y las palabras clave que comparten.
     *
     * @param peliculaId el id de TMDB de la pelicula de la que se parte.
     * @param alTerminar recibe las parecidas, o un fallo si no se pudo pedir.
     */
    fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Peliculas que le gustaron a quien vio esta.
     *
     * No es lo mismo que [similares]: aquellas se parecen en los datos, estas salen de lo que
     * la gente ve de verdad. Por eso son las que alimentan el "porque te gusto X" de la portada.
     *
     * @param peliculaId el id de TMDB de la pelicula de la que se parte.
     * @param alTerminar recibe las recomendadas, o un fallo si no se pudo pedir.
     */
    fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Las reseñas que ha escrito la gente de TMDB sobre una pelicula.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe las reseñas con texto, que las vacias no aportan nada.
     */
    fun resenas(peliculaId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit)

    /**
     * Peliculas en las que sale alguna de esas personas, delante o detras de la camara.
     *
     * @param personas ids de TMDB. Basta con que salga una de ellas, no hacen falta todas.
     * @param alTerminar recibe las peliculas de esa gente, o un fallo si no se pudo pedir.
     */
    fun deLaGente(personas: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit)

    /**
     * Peliculas de un genero, ordenadas por popularidad. Alimenta la recomendacion.
     *
     * @param generos ids de genero. Se piden todos a la vez, no cualquiera de ellos: con dos
     *   generos salen las que son las dos cosas, que es lo que afina la recomendacion.
     * @param pagina cual de las tandas de 20 se pide.
     * @param alTerminar recibe las peliculas, o un fallo si no se pudo pedir.
     */
    fun porGeneros(
        generos: List<Int>,
        pagina: Int = 1,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    )

    /**
     * Los generos de cine con sus ids.
     *
     * @param alTerminar recibe los generos con el nombre ya en el idioma del aparato. El id es
     *   lo que hay que guardar en los gustos y mandar luego a [porGeneros] o a [descubrir].
     */
    fun generos(alTerminar: (Resultado<List<com.example.dailymovie.models.GenreModel>>) -> Unit)

    /**
     * Peliculas que cumplen unos filtros, sin que haga falta escribir nada.
     *
     * @param generos ids de genero, o vacio para no filtrar por genero.
     * @param filtros año, nota minima y plataforma. Los que vengan a null no se mandan.
     * @param pagina cual de las tandas de 20 se pide.
     * @param alTerminar recibe las peliculas que pasan el filtro, o un fallo si no se pudo
     *   pedir. Si no hay ninguna que lo cumpla llega una lista vacia, que no es lo mismo.
     */
    fun descubrir(
        generos: List<Int> = emptyList(),
        filtros: FiltrosAvanzados = FiltrosAvanzados(),
        pagina: Int = 1,
        alTerminar: (Resultado<List<MovieModel>>) -> Unit
    )

    /**
     * Las plataformas de streaming del pais del aparato, para poder filtrar por ellas.
     *
     * @param alTerminar recibe las plataformas ya ordenadas por lo usadas que son en ese pais,
     *   o un fallo si no se pudo pedir.
     */
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

    /** Salio bien y estos son los datos. Que vengan vacios sigue siendo un exito. */
    data class Exito<T : Any>(val datos: T) : Resultado<T>()

    /**
     * No hay datos, y aqui esta el porque.
     *
     * El motivo es un enum y no un texto a proposito: asi el ViewModel dice que fallo sin
     * tocar recursos de Android, y es la vista la que elige que mensaje enseñar.
     */
    data class Fallo(val motivo: ErrorCarga) : Resultado<Nothing>()
}

/**
 * Atajo para no repetir el when cuando solo interesa el caso bueno.
 *
 * @param bloque se ejecuta con los datos dentro, y solo si el resultado fue un exito.
 * @return el mismo resultado sin tocar, para poder encadenar un [siFalla] detras.
 */
inline fun <T : Any> Resultado<T>.siVaBien(bloque: (T) -> Unit): Resultado<T> {
    if (this is Resultado.Exito) bloque(datos)
    return this
}

/**
 * Atajo para el caso malo.
 *
 * @param bloque se ejecuta con el motivo del fallo, y solo si el resultado fue un fallo.
 * @return el mismo resultado sin tocar, para poder encadenarlo.
 */
inline fun <T : Any> Resultado<T>.siFalla(bloque: (ErrorCarga) -> Unit): Resultado<T> {
    if (this is Resultado.Fallo) bloque(motivo)
    return this
}
