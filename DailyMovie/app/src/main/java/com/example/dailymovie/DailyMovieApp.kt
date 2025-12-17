package com.example.dailymovie

import android.app.Application
import com.example.dailymovie.utils.IdiomaDelContenido

/**
 * El arranque de la app, antes de que se pinte ninguna pantalla.
 *
 * Existe por una razon muy concreta: el idioma en el que se le pide todo a TMDB hay que
 * tenerlo cargado **antes** de la primera peticion, y quien lo pregunta (`WebService`, en los
 * valores por defecto de sus parametros) no tiene ningun Context. Aqui se carga una vez y ya
 * esta disponible para todo lo demas.
 */
class DailyMovieApp : Application() {

    override fun onCreate() {
        super.onCreate()
        IdiomaDelContenido.inicializar(this)
    }
}
