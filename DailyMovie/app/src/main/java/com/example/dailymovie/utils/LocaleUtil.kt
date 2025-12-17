package com.example.dailymovie.utils

import java.util.Locale

/**
 * En que idioma y de que pais se le pide todo a TMDB.
 *
 * Idioma y pais no son lo mismo y aqui hacen falta los dos por separado: el idioma decide en
 * que lengua llegan titulos y sinopsis, y el pais decide en que plataformas se puede ver algo,
 * que cambia de un sitio a otro aunque se hable igual.
 *
 * Por eso cada uno se saca de un sitio distinto:
 *  - **El idioma** puede haberlo cambiado el usuario en Ajustes, asi que primero se mira
 *    [IdiomaDelContenido] y solo si no ha elegido nada se coge el del aparato.
 *  - **El pais** siempre es el del aparato, pase lo que pase con el idioma. Elegir "ver las
 *    peliculas en ingles" no significa haberse mudado a Estados Unidos: las plataformas y la
 *    clasificacion por edad tienen que seguir siendo las de aqui.
 */
object LocaleUtil {

    /** @return el idioma en dos letras ("es"): el elegido en Ajustes o, si no, el del aparato. */
    fun getDeviceLanguage(): String =
        IdiomaDelContenido.etiquetaEnUso() ?: Locale.getDefault().language

    /**
     * @return el pais del aparato en dos letras ("ES"). Es la clave con la que se buscan las
     *   plataformas de streaming, que TMDB devuelve agrupadas por pais.
     */
    fun getDeviceCountry(): String = Locale.getDefault().country

    /** @return los dos juntos ("es-ES"), que es el formato del `language` de TMDB. */
    fun getLanguageAndCountry(): String = "${getDeviceLanguage()}-${getDeviceCountry()}"
}
