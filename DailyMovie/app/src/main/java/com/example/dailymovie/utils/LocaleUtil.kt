package com.example.dailymovie.utils

import java.util.Locale

/**
 * En que idioma y de que pais se le pide todo a TMDB.
 *
 * Se lee del aparato y no de un ajuste de la app: si el movil esta en español, la app tiene
 * que salir en español sin que nadie elija nada.
 *
 * Idioma y pais no son lo mismo y aqui hacen falta los dos por separado: el idioma decide en
 * que lengua llegan titulos y sinopsis, y el pais decide en que plataformas se puede ver algo,
 * que cambia de un sitio a otro aunque se hable igual.
 */
object LocaleUtil {
    /** @return el idioma del aparato en dos letras ("es"). */
    fun getDeviceLanguage(): String {
        return Locale.getDefault().language
    }

    /**
     * @return el pais del aparato en dos letras ("ES"). Es la clave con la que se buscan las
     *   plataformas de streaming, que TMDB devuelve agrupadas por pais.
     */
    fun getDeviceCountry(): String {
        return Locale.getDefault().country
    }

    /** @return los dos juntos ("es-ES"), que es el formato del `language` de TMDB. */
    fun getLanguageAndCountry(): String {
        return "${getDeviceLanguage()}-${getDeviceCountry()}"
    }
}