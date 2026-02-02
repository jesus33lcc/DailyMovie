package com.example.dailymovie

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
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
        prepararFirestore()
    }

    /**
     * La cache offline de Firestore: lo que ya se ha visto sigue estando sin cobertura.
     *
     * Va aqui y no en el constructor del repositorio porque `firestoreSettings` solo se puede
     * tocar **antes** de la primera operacion; si alguien llegara a Firestore antes de que se
     * creara `Dependencias.usuario`, esto lanzaba IllegalStateException ("already been
     * started") y la app no arrancaba. En el arranque pasa una vez y en un sitio previsible.
     *
     * Se usa setLocalCacheSettings y no el setPersistenceEnabled de toda la vida porque ese
     * quedo obsoleto; hacen lo mismo, pero el nuevo deja elegir el tamaño de la cache si algun
     * dia hace falta.
     */
    private fun prepararFirestore() {
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }
}
