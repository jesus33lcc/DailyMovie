package com.example.dailymovie.utils

import com.example.dailymovie.BuildConfig

/**
 * Las direcciones fijas de TMDB y la clave de la API.
 *
 * Estan todas juntas para que cambiar un tamaño de imagen o una direccion sea tocar una linea
 * y no ir buscandola por medio proyecto.
 */
object Constantes {
    /**
     * La clave llega desde local.properties al compilar (ver app/build.gradle.kts), asi no
     * se queda escrita en el codigo. Se mantiene aqui para no cambiar quien la usa.
     */
    val API_KEY: String = BuildConfig.TMDB_API_KEY

    /** La raiz de la API v3; de aqui cuelgan todos los endpoints de `WebService`. */
    const val BASE_URL = "https://api.themoviedb.org/3/";

    /**
     * Imagenes al tamaño de siempre: carteles, fotos de gente y fotogramas.
     *
     * TMDB devuelve solo el trozo final de la ruta ("/abc123.jpg"), asi que hay que pegarle
     * esto delante. El w500 es el punto medio entre que se vea bien y que no tarde.
     */
    const val IMAGE_URL = "https://image.tmdb.org/t/p/w500/"

    /** Para la galeria a pantalla completa, donde el w500 se ve pixelado. */
    const val IMAGE_ORIGINAL_URL = "https://image.tmdb.org/t/p/original/"

    /** Para abrir la ficha en IMDb. Se le pega el `imdb_id`, que no es el id de TMDB. */
    const val IMDB_URL = "https://www.imdb.com/title/"

    /** Para abrir un trailer en el navegador cuando no esta la app de YouTube. */
    const val YOUTUBE_URL = "https://www.youtube.com/watch?v="

    /** La pagina publica de una pelicula: es lo que se manda al compartirla. */
    const val BASE_MOVIE_URL = "https://www.themoviedb.org/movie/"

    /** Igual pero de una serie. TMDB las llama "tv", no "serie". */
    const val BASE_SERIE_URL = "https://www.themoviedb.org/tv/"
}