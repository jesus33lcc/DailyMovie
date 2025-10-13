// WebService.kt
package com.example.dailymovie.client

import com.example.dailymovie.client.response.*
import com.example.dailymovie.utils.LocaleUtil
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WebService {
    @GET("search/movie")
    fun searchMovies(
        @Query("query") title: String,
        @Query("api_key") apiKey: String,
        @Query("include_adult") include_adult: Boolean = true,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        // append_to_response trae imagenes, clasificacion por edad e ids externos en la
        // misma peticion, en vez de hacer tres viajes mas a TMDB.
        @Query("append_to_response") extras: String = "images,release_dates,external_ids",
        // Las imagenes no llevan idioma, asi que hay que pedirlas aparte o vienen vacias.
        @Query("include_image_language") idiomasDeImagen: String = "es,en,null"
    ): Call<MovieDetailsResponse>

    @GET("movie/now_playing")
    fun getNowPlayingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/popular")
    fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/top_rated")
    fun getTopRatedMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/upcoming")
    fun getUpcomingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/{movie_id}/watch/providers")
    fun getMovieProviders(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<ProviderResponse>

    @GET("movie/{movie_id}/credits")
    fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Call<CreditResponse>

    @GET("movie/{movie_id}/videos")
    fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<VideoResponse>

    @GET("movie/{movie_id}/similar")
    fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("movie/{movie_id}/recommendations")
    fun getRecommendedMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    // ---- Buscar cualquier cosa ----

    /**
     * El unico endpoint que busca peliculas, series y gente a la vez, ya ordenadas por lo
     * que mas encaja con lo escrito. Antes se usaba search/movie, que solo veia peliculas.
     */
    @GET("search/multi")
    fun buscarTodo(
        @Query("query") consulta: String,
        @Query("api_key") apiKey: String,
        @Query("include_adult") incluirAdulto: Boolean = false,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<BusquedaMultiResponse>

    /** Lo que esta petando esta semana, para tener algo que enseñar sin buscar nada. */
    @GET("trending/all/week")
    fun getTendencias(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<TendenciasResponse>

    // ---- Recomendacion ----

    @GET("discover/movie")
    fun descubrirPeliculas(
        @Query("api_key") apiKey: String,
        @Query("with_genres") generos: String,
        @Query("sort_by") orden: String = "popularity.desc",
        @Query("vote_count.gte") votosMinimos: Int = 300,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<MoviesResponse>

    @GET("genre/movie/list")
    fun getGeneros(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<GenresResponse>

    /**
     * Los generos de series.
     *
     * Van aparte de los de cine porque TMDB usa otros ids: "Accion" es el 28 en peliculas,
     * pero en series no existe como tal y esta dentro de "Action & Adventure", que es el
     * 10759. Con el id de pelicula, discover/tv devuelve cualquier cosa.
     */
    @GET("genre/tv/list")
    fun getGenerosDeSeries(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<GenresResponse>

    @GET("discover/tv")
    fun descubrirSeries(
        @Query("api_key") apiKey: String,
        @Query("with_genres") generos: String,
        @Query("sort_by") orden: String = "popularity.desc",
        @Query("vote_count.gte") votosMinimos: Int = 100,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    // ---- Personas (actores y directores) ----

    @GET("person/{person_id}")
    fun getPersona(
        @Path("person_id") personaId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<PersonResponse>

    @GET("person/{person_id}/movie_credits")
    fun getFilmografia(
        @Path("person_id") personaId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<PersonCreditsResponse>

    // ---- Series ----

    @GET("search/tv")
    fun buscarSeries(
        @Query("query") titulo: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    @GET("tv/popular")
    fun getSeriesPopulares(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    @GET("tv/top_rated")
    fun getSeriesMejorValoradas(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    @GET("tv/on_the_air")
    fun getSeriesEnEmision(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry(),
        @Query("page") page: Int = 1
    ): Call<SeriesResponse>

    @GET("tv/{tv_id}")
    fun getSerieDetalles(
        @Path("tv_id") serieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<SerieDetailsResponse>

    @GET("tv/{tv_id}/season/{season_number}")
    fun getTemporada(
        @Path("tv_id") serieId: Int,
        @Path("season_number") numeroTemporada: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<SeasonResponse>

    @GET("tv/{tv_id}/credits")
    fun getSerieCreditos(
        @Path("tv_id") serieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<CreditResponse>

    @GET("tv/{tv_id}/videos")
    fun getSerieVideos(
        @Path("tv_id") serieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = LocaleUtil.getLanguageAndCountry()
    ): Call<VideoResponse>

    @GET("tv/{tv_id}/watch/providers")
    fun getSerieProviders(
        @Path("tv_id") serieId: Int,
        @Query("api_key") apiKey: String
    ): Call<ProviderResponse>
}
