package com.example.dailymovie.client

import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.LocaleUtil
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Le pone a cada peticion la clave de TMDB y el idioma.
 *
 * Antes iban como parametro en cada endpoint: `api_key` estaba escrito 37 veces en
 * `WebService` y `language` 29, y eso obligaba a que los tres repositorios recibieran la
 * clave por constructor y la pasaran en cada llamada. Total, mas de sesenta sitios donde
 * escribir siempre lo mismo y uno donde olvidarlo.
 *
 * Puesto aqui, no hay forma de que a una peticion se le olvide.
 */
class ClaveEIdioma : Interceptor {

    override fun intercept(cadena: Interceptor.Chain): Response {
        val original = cadena.request()
        val direccion = original.url().newBuilder()
            .addQueryParameter("api_key", Constantes.API_KEY)

        // El idioma solo si no lo trae ya: hay endpoints que lo mandan a proposito distinto,
        // como los videos, que se piden tambien en ingles porque muchos trailers solo estan
        // ahi. Si se añadiera igualmente, la peticion iria con dos "language" y TMDB elegiria
        // uno de los dos sin decir cual.
        if (original.url().queryParameter("language") == null) {
            direccion.addQueryParameter("language", LocaleUtil.getLanguageAndCountry())
        }

        return cadena.proceed(original.newBuilder().url(direccion.build()).build())
    }
}
