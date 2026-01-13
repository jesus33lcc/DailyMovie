// WebService.kt
package com.example.dailymovie.client

import com.example.dailymovie.client.response.*
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Los endpoints de TMDB que usa la app, tal cual los entiende Retrofit.
 *
 * Es la capa mas baja: aqui no se decide nada, solo se describe que URL hay que pedir y con
 * que parametros. Quien filtra, ordena o traduce lo que llega son los repositorios, y son los
 * unicos que deberian llamar aqui. Una vista o un ViewModel que importe esto se salta la
 * interfaz del repositorio y deja de poder probarse sin internet.
 *
 * Tres cosas que se repiten en casi todas las funciones y conviene saber una sola vez:
 *
 *  - **Ninguna funcion sale a la red al llamarla.** Devuelven un `Call` sin disparar, y quien
 *    lo lanza es `enqueueSimple`, que es donde estan resueltos los fallos.
 *  - **Aqui no se ve la clave de TMDB ni el idioma.** Los pone [ClaveEIdioma] a toda peticion
 *    que salga, asi que no hay forma de que a un endpoint se le olviden. Estaban escritos a
 *    mano en cada uno: `api_key` 37 veces y `language` 29.
 *  - **El `language` solo se declara donde tiene que ser otro**, como en los videos, que se
 *    piden tambien en ingles. En el resto lo pone el interceptor con el del aparato.
 *  - **`page` viene con la primera tanda**, y se pasa solo al pedir mas.
 */
interface WebService {

    /**
     * La ficha completa de una pelicula.
     *
     * @param movieId el id de TMDB de la pelicula.
     * @param language idioma de la sinopsis y el titulo; por defecto el del aparato.
     * @param extras que mas se trae de paso en la misma peticion. Cambiarlo deja campos de
     *   [MovieDetailsResponse] a null, porque son justo los que llegan por aqui.
     * @param idiomasDeImagen de que idiomas se quieren los carteles y logos. El "null" del
     *   final no es un despiste: asi TMDB incluye las imagenes sin idioma, que son la mayoria
     *   de los fondos.
     * @return los detalles con la galeria, la clasificacion por edad y los ids externos dentro.
     */
    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        // append_to_response trae imagenes, clasificacion por edad e ids externos en la
        // misma peticion, en vez de hacer tres viajes mas a TMDB.
        @Query("append_to_response") extras: String = "images,release_dates,external_ids",
        // Las imagenes no llevan idioma, asi que hay que pedirlas aparte o vienen vacias.
        @Query("include_image_language") idiomasDeImagen: String = "es,en,null"
    ): Call<MovieDetailsResponse>

    /**
     * Lo que hay ahora mismo en los cines.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/now_playing")
    fun getNowPlayingMovies(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Lo mas visto del momento. Es la red de seguridad de la portada: siempre devuelve algo.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/popular")
    fun getPopularMovies(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Las mejor valoradas de siempre segun los votos de TMDB.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/top_rated")
    fun getTopRatedMovies(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Lo que esta por estrenarse.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/upcoming")
    fun getUpcomingMovies(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Donde se puede ver una pelicula: suscripcion, alquiler o compra.
     *
     * No lleva idioma porque la respuesta viene partida por paises, y del monton hay que
     * quedarse con el del aparato.
     *
     * @param movieId el id de TMDB de la pelicula.
     * @return las plataformas de todos los paises a la vez, con el codigo de pais como clave.
     */
    @GET("movie/{movie_id}/watch/providers")
    fun getMovieProviders(
        @Path("movie_id") movieId: Int,
    ): Call<ProviderResponse>

    /**
     * Quien sale y quien trabaja en una pelicula.
     *
     * @param movieId el id de TMDB de la pelicula.
     * @return el reparto y el equipo por separado, que TMDB los da en dos listas distintas.
     */
    @GET("movie/{movie_id}/credits")
    fun getMovieCredits(
        @Path("movie_id") movieId: Int,
    ): Call<CreditResponse>

    /**
     * Los videos de una pelicula: trailers, clips y detras de las camaras.
     *
     * @param movieId el id de TMDB de la pelicula.
     * @param language de que idioma se quieren. Aqui si se pasa a mano: TMDB tiene muchisimos
     *   mas trailers en "en-US" que en español, asi que el repositorio pide las dos cosas.
     */
    @GET("movie/{movie_id}/videos")
    fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<VideoResponse>

    /**
     * Peliculas parecidas, segun los generos y las palabras clave que comparten.
     *
     * @param movieId el id de TMDB de la pelicula de la que se parte.
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/{movie_id}/similar")
    fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Peliculas que le gustaron a quien vio esta.
     *
     * No es lo mismo que [getSimilarMovies]: aquellas se parecen en los datos, estas salen de
     * lo que de verdad ve la gente. Por eso son las que alimentan el "porque te gusto X".
     *
     * @param movieId el id de TMDB de la pelicula de la que se parte.
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("movie/{movie_id}/recommendations")
    fun getRecommendedMovies(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    // ---- Buscar cualquier cosa ----

    /**
     * El unico endpoint que busca peliculas, series y gente a la vez, ya ordenadas por lo
     * que mas encaja con lo escrito. Antes se usaba search/movie, que solo veia peliculas.
     *
     * @param consulta lo que ha escrito el usuario.
     * @param incluirAdulto aqui va a false a proposito, que esto es la busqueda general.
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide. Es lo que permite ir cargando segun se baja.
     * @return los tres tipos mezclados en la misma lista; hay que mirar `media_type` de cada
     *   resultado para saber que es.
     */
    @GET("search/multi")
    fun buscarTodo(
        @Query("query") consulta: String,
        @Query("include_adult") incluirAdulto: Boolean = false,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<BusquedaMultiResponse>

    /**
     * La gente del cine mas conocida ahora mismo.
     *
     * Se usa en el onboarding cuando el usuario no ha marcado ninguna pelicula: sin nada de
     * donde tirar, las caras populares son lo unico que le va a sonar.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("person/popular")
    fun getPersonasPopulares(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<PersonasPopularesResponse>

    /**
     * Sagas por nombre.
     *
     * search/multi no las devuelve nunca, asi que buscando "El Padrino" jamas aparecia la
     * triologia entera: habia que ir pelicula por pelicula.
     *
     * @param consulta lo que ha escrito el usuario.
     * @param language idioma de los textos; por defecto el del aparato.
     * @return solo la cabecera de cada saga (nombre y cartel), sin las peliculas de dentro:
     *   para eso hay que ir luego a [getSaga].
     */
    @GET("search/collection")
    fun buscarSagas(
        @Query("query") consulta: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<BusquedaDeSagasResponse>

    /**
     * Las peliculas de una saga, por orden de estreno.
     *
     * @param sagaId el id de coleccion de TMDB, que no tiene nada que ver con el de pelicula.
     * @param language idioma de los textos; por defecto el del aparato.
     */
    @GET("collection/{id}")
    fun getSaga(
        @Path("id") sagaId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<DetalleDeSagaResponse>

    /**
     * Lo que esta petando esta semana, para tener algo que enseñar sin buscar nada.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @return peliculas, series y gente mezcladas, igual que [buscarTodo], y por eso comparten
     *   el mismo tipo de resultado.
     */
    @GET("trending/all/week")
    fun getTendencias(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<TendenciasResponse>

    // ---- Recomendacion ----

    /**
     * Peliculas que cumplen unos filtros.
     *
     * Los parametros con null no se mandan, asi que quien no filtra por algo no paga por ello:
     * Retrofit se salta las consultas nulas y TMDB devuelve lo de siempre.
     *
     * @param generos ids separados por coma para pedir que esten todos, o por "|" para que
     *   valga con cualquiera. El repositorio usa la coma, que afina mas.
     * @param orden como se ordena lo que vuelve, en el formato de TMDB (`campo.desc`).
     * @param votosMinimos suelo de votos para que la nota signifique algo. Sin esto, discover
     *   se llena de peliculas con un 10 puesto por cuatro personas.
     * @param notaMinima nota de 0 a 10, o null para no filtrar.
     * @param ano solo lo estrenado ese año, o null para no filtrar.
     * @param plataformas ids de plataforma de streaming. Si va, hay que mandar tambien
     *   [region] o TMDB no sabe de que pais le hablas.
     * @param gente ids de actores o directores.
     * @param region codigo de pais para las plataformas.
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("discover/movie")
    fun descubrirPeliculas(
        @Query("with_genres") generos: String? = null,
        @Query("sort_by") orden: String = "popularity.desc",
        @Query("vote_count.gte") votosMinimos: Int = 300,
        @Query("vote_average.gte") notaMinima: Double? = null,
        @Query("primary_release_year") ano: Int? = null,
        @Query("with_watch_providers") plataformas: String? = null,
        // Sale en la pelicula, delante o detras de la camara. Alimenta el "porque sigues a
        // Nolan" de la portada.
        @Query("with_people") gente: String? = null,
        @Query("watch_region") region: String? = null,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    /**
     * Las plataformas de streaming que hay en el pais del usuario.
     *
     * @param region de que pais se quieren; por defecto el del aparato. Es importante: los ids
     *   de plataforma no son los mismos en todos los paises.
     * @param language idioma de los textos; por defecto el del aparato.
     */
    @GET("watch/providers/movie")
    fun getPlataformasDisponibles(
        @Query("watch_region") region: String = LocaleUtil.getDeviceCountry(),
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<PlataformasResponse>

    /**
     * Los generos de cine con sus ids, que es lo que hay que mandar luego a discover.
     *
     * @param language en que idioma vienen los nombres; por defecto el del aparato.
     */
    @GET("genre/movie/list")
    fun getGeneros(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<GenresResponse>

    /**
     * Los generos de series.
     *
     * Van aparte de los de cine porque TMDB usa otros ids: "Accion" es el 28 en peliculas,
     * pero en series no existe como tal y esta dentro de "Action & Adventure", que es el
     * 10759. Con el id de pelicula, discover/tv devuelve cualquier cosa.
     *
     * @param language en que idioma vienen los nombres; por defecto el del aparato.
     */
    @GET("genre/tv/list")
    fun getGenerosDeSeries(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<GenresResponse>

    /**
     * Igual que [descubrirPeliculas] pero de series, con sus propios campos de fecha.
     *
     * @param generos ids de genero de series, que no son los de cine.
     * @param orden como se ordena lo que vuelve, en el formato de TMDB (`campo.desc`).
     * @param votosMinimos suelo de votos. Aqui es mas bajo que en cine porque las series
     *   reunen menos votos y con 300 se quedaban fuera casi todas.
     * @param notaMinima nota de 0 a 10, o null para no filtrar.
     * @param ano solo lo que empezo a emitirse ese año, o null para no filtrar.
     * @param plataformas ids de plataforma de streaming; necesita [region].
     * @param gente ids de actores o directores.
     * @param region codigo de pais para las plataformas.
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("discover/tv")
    fun descubrirSeries(
        @Query("with_genres") generos: String? = null,
        @Query("sort_by") orden: String = "popularity.desc",
        @Query("vote_count.gte") votosMinimos: Int = 100,
        @Query("vote_average.gte") notaMinima: Double? = null,
        @Query("first_air_date_year") ano: Int? = null,
        @Query("with_watch_providers") plataformas: String? = null,
        // Sale en la pelicula, delante o detras de la camara. Alimenta el "porque sigues a
        // Nolan" de la portada.
        @Query("with_people") gente: String? = null,
        @Query("watch_region") region: String? = null,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    // ---- Personas (actores y directores) ----

    /**
     * La ficha de una persona: nombre, foto, biografia y fechas.
     *
     * @param personaId el id de TMDB de la persona.
     * @param language idioma de la biografia; por defecto el del aparato. Ojo: TMDB devuelve
     *   el oficio siempre en ingles aunque se le pida en español, y eso se traduce a mano
     *   mas arriba.
     */
    @GET("person/{person_id}")
    fun getPersona(
        @Path("person_id") personaId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<PersonResponse>

    /**
     * Todo lo que ha hecho una persona: peliculas y series.
     *
     * Antes se pedia movie_credits, asi que en la ficha de un actor de series no salia nada.
     * combined_credits trae las dos cosas y dice en media_type cual es cada una.
     *
     * @param personaId el id de TMDB de la persona.
     * @param language idioma de los titulos; por defecto el del aparato.
     * @return todo en bruto y sin filtrar, incluidos promocionales y apariciones de un minuto.
     *   Quien decide que merece la pena enseñar es el repositorio.
     */
    /**
     * Las fotos que TMDB tiene de una persona.
     *
     * Son retratos verticales, no fotogramas: la galeria las enseña igual, lo unico que
     * cambia es la forma.
     *
     * @param personaId el id de TMDB de la persona.
     */
    @GET("person/{person_id}/images")
    fun getImagenesDePersona(
        @Path("person_id") personaId: Int,
    ): Call<ImagenesDePersonaResponse>

    @GET("person/{person_id}/combined_credits")
    fun getFilmografia(
        @Path("person_id") personaId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<PersonCreditsResponse>

    /**
     * Lo que ha escrito la gente sobre una pelicula.
     *
     * No se pide con append_to_response junto a los detalles porque solo se necesitan al
     * bajar del todo en la ficha, y cargarlas de golpe haria la primera peticion mas pesada
     * para todo el mundo.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @return las reseñas tal cual, casi siempre en ingles: no lleva idioma porque las escribe
     *   la gente de TMDB y no hay traduccion que pedir.
     */
    @GET("movie/{movie_id}/reviews")
    fun getResenas(
        @Path("movie_id") peliculaId: Int,
    ): Call<ResenasResponse>

    // ---- Series ----

    /**
     * Las series mas vistas del momento.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("tv/popular")
    fun getSeriesPopulares(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    /**
     * Las series mejor valoradas segun los votos de TMDB.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("tv/top_rated")
    fun getSeriesMejorValoradas(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    /**
     * Las series que estan emitiendo capitulos estos dias.
     *
     * @param language idioma de los textos; por defecto el del aparato.
     * @param page que tanda de 20 se pide.
     */
    @GET("tv/on_the_air")
    fun getSeriesEnEmision(
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    /**
     * La ficha de una serie, con la lista de temporadas.
     *
     * Las temporadas vienen solo enumeradas, sin los episodios dentro: para eso hay que pedir
     * cada una con [getTemporada].
     *
     * @param serieId el id de TMDB de la serie.
     * @param language idioma de la sinopsis y el titulo; por defecto el del aparato.
     */
    @GET("tv/{tv_id}")
    fun getSerieDetalles(
        @Path("tv_id") serieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        // Trae el id de IMDb en la misma peticion, en vez de hacer otro viaje solo para eso.
        @Query("append_to_response") extras: String = "external_ids"
    ): Call<SerieDetailsResponse>

    /**
     * Una temporada con todos sus episodios.
     *
     * @param serieId el id de TMDB de la serie.
     * @param numeroTemporada el numero que le da TMDB. La 0 existe y son los especiales, asi
     *   que no se puede dar por hecho que la primera sea la 1.
     * @param language idioma de los textos; por defecto el del aparato.
     */
    /**
     * Series parecidas a una, segun TMDB.
     *
     * @param serieId el id de TMDB de la serie.
     */
    @GET("tv/{tv_id}/similar")
    fun getSeriesSimilares(
        @Path("tv_id") serieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<SeriesResponse>

    /**
     * Las que TMDB recomienda a quien ha visto esa.
     *
     * No es lo mismo que las similares: las similares se parecen en genero y tono, y estas
     * salen de lo que ve la gente que ve esa serie.
     *
     * @param serieId el id de TMDB de la serie.
     */
    @GET("tv/{tv_id}/recommendations")
    fun getSeriesRecomendadas(
        @Path("tv_id") serieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<SeriesResponse>

    /**
     * Las imagenes que TMDB tiene de una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param idiomasDeImagen las imagenes no llevan idioma por defecto, asi que hay que
     *   pedirlas asi o vienen vacias.
     */
    @GET("tv/{tv_id}/images")
    fun getImagenesDeSerie(
        @Path("tv_id") serieId: Int,
        @Query("include_image_language") idiomasDeImagen: String = "es,en,null"
    ): Call<ImagenesResponse>

    /**
     * La clasificacion por edad de una serie, pais por pais.
     *
     * En peliculas viene dentro de los detalles con append_to_response; en series TMDB lo
     * pone en un endpoint aparte, asi que hay que pedirlo a mano.
     *
     * @param serieId el id de TMDB de la serie.
     * @return la lista de paises con su clasificacion. Hay que buscar la del pais del
     *   usuario; el resto no le dicen nada.
     */
    @GET("tv/{tv_id}/content_ratings")
    fun getClasificacionSerie(
        @Path("tv_id") serieId: Int,
    ): Call<ClasificacionesSerieResponse>

    /**
     * Lo que ha escrito la gente sobre una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @return las reseñas, casi siempre en ingles: es el idioma en el que las escriben.
     */
    @GET("tv/{tv_id}/reviews")
    fun getResenasSerie(
        @Path("tv_id") serieId: Int,
    ): Call<ResenasResponse>

    @GET("tv/{tv_id}/season/{season_number}")
    fun getTemporada(
        @Path("tv_id") serieId: Int,
        @Path("season_number") numeroTemporada: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<SeasonResponse>

    /**
     * El reparto de una serie, contando toda la emision y no un capitulo suelto.
     *
     * @param serieId el id de TMDB de la serie.
     * @param language idioma de los textos; por defecto el del aparato.
     */
    @GET("tv/{tv_id}/credits")
    fun getSerieCreditos(
        @Path("tv_id") serieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<CreditResponse>

    /**
     * Los videos de una serie: trailers, adelantos y clips.
     *
     * @param serieId el id de TMDB de la serie.
     * @param language de que idioma se quieren; por defecto el del aparato. A diferencia de
     *   las peliculas, aqui no se pide una segunda vuelta en ingles.
     */
    @GET("tv/{tv_id}/videos")
    fun getSerieVideos(
        @Path("tv_id") serieId: Int,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<VideoResponse>

    /**
     * Donde se puede ver una serie.
     *
     * No lleva idioma, igual que en peliculas: la respuesta viene partida por paises y hay que
     * quedarse con el del aparato.
     *
     * @param serieId el id de TMDB de la serie.
     * @return las plataformas de todos los paises, con el codigo de pais como clave.
     */
    @GET("tv/{tv_id}/watch/providers")
    fun getSerieProviders(
        @Path("tv_id") serieId: Int,
    ): Call<ProviderResponse>
}
