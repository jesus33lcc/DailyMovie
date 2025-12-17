package com.example.dailymovie.utils

import android.content.Context

/**
 * Uno de los idiomas que se pueden elegir en Ajustes.
 *
 * @property etiqueta el código de dos letras que entiende TMDB ("es"), o null para decir
 *   "el mismo que el móvil".
 * @property nombre cómo se llama en el diálogo. Va escrito en su propio idioma, que es lo que
 *   reconoce quien lo busca: alguien que quiere las películas en francés busca "Français",
 *   no "Francés".
 */
enum class Idioma(val etiqueta: String?, val nombre: String) {
    DEL_MOVIL(null, "El del móvil"),
    ESPANOL("es", "Español"),
    INGLES("en", "English"),
    FRANCES("fr", "Français"),
    ITALIANO("it", "Italiano"),
    PORTUGUES("pt", "Português"),
    ALEMAN("de", "Deutsch")
}

/**
 * En qué idioma se le piden las películas a TMDB.
 *
 * **Se guarda a mano y no se toca el idioma del sistema a propósito.** Android 13 trae una API
 * para dar a cada app su idioma (`AppCompatDelegate.setApplicationLocales`), y era la opción
 * evidente, pero rompe algo que aquí importa: al ponerle a la app un idioma suelto como "en",
 * el `Locale` por defecto se queda **sin país**, y de ahí sale la clave con la que se buscan
 * las plataformas de streaming y la clasificación por edad. Resultado: elegir "English" hacía
 * desaparecer el "Dónde verla" y el "16" de las fichas.
 *
 * Guardándolo aquí, el idioma solo afecta a lo que se le pide a TMDB, que es justo lo que se
 * quería, y el país sigue siendo el del móvil: ver las películas en inglés no significa
 * haberse mudado.
 *
 * Lo que **no** hace: traducir la app. Los botones y los avisos están escritos en español.
 * Por eso en Ajustes se llama "idioma de las películas" y no "idioma de la app".
 */
object IdiomaDelContenido {

    /**
     * El idioma elegido, en memoria.
     *
     * Hace falta tenerlo aqui y no solo en las preferencias porque quien lo pregunta es
     * `LocaleUtil`, y a `LocaleUtil` lo llama `WebService` desde los valores por defecto de
     * sus parametros: ahi no hay ningun Context al que agarrarse. Lo carga [inicializar] al
     * arrancar la app.
     */
    @Volatile
    private var enMemoria: String? = null

    /** Carga el idioma guardado. Lo llama la clase Application al arrancar. */
    fun inicializar(context: Context) {
        enMemoria = preferencias(context).getString(CLAVE, null)
    }

    /** @return las dos letras del idioma elegido, o null si se sigue al movil. */
    fun etiquetaEnUso(): String? = enMemoria

    /**
     * El que está puesto ahora.
     *
     * @param context cualquiera vale, solo se usa para llegar a las preferencias.
     * @return el idioma elegido, o [Idioma.DEL_MOVIL] si nunca se ha tocado.
     */
    fun elPuesto(context: Context): Idioma {
        val guardado = preferencias(context).getString(CLAVE, null) ?: return Idioma.DEL_MOVIL
        return Idioma.entries.firstOrNull { it.etiqueta == guardado } ?: Idioma.DEL_MOVIL
    }

    /**
     * Cambia el idioma en el que se pide todo a TMDB.
     *
     * Quien llame a esto tiene que refrescar la pantalla después: lo que ya está cargado se
     * pidió en el idioma de antes y no se entera de nada.
     *
     * @param context cualquiera vale.
     * @param idioma el elegido. Con [Idioma.DEL_MOVIL] se vuelve a seguir al móvil.
     */
    fun cambiar(context: Context, idioma: Idioma) {
        enMemoria = idioma.etiqueta
        preferencias(context).edit().apply {
            if (idioma.etiqueta == null) remove(CLAVE) else putString(CLAVE, idioma.etiqueta)
        }.apply()
    }

    private fun preferencias(context: Context) =
        context.applicationContext.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)

    private const val FICHERO = "idioma_del_contenido"
    private const val CLAVE = "idioma"
}
