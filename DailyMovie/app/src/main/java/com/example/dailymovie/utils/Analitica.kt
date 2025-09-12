package com.example.dailymovie.utils

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Interruptor de la analitica.
 *
 * Google Play obliga a pedir consentimiento para recoger datos de uso y a que se pueda
 * retirar. Antes Firebase Analytics venia activado sin preguntar nada, que ademas de ser
 * mala practica es motivo de rechazo en la revision.
 *
 * Se arranca desactivada: hasta que el usuario diga que si, no se manda nada.
 */
object Analitica {

    private const val FICHERO = "ajustes_dailymovie"
    private const val CLAVE = "analitica_activada"

    fun estaActivada(context: Context): Boolean =
        preferencias(context).getBoolean(CLAVE, false)

    fun activar(context: Context, activada: Boolean) {
        preferencias(context).edit().putBoolean(CLAVE, activada).apply()
        aplicar(context, activada)
    }

    /** Se llama al arrancar la app para respetar lo que el usuario eligio la ultima vez. */
    fun aplicarLoGuardado(context: Context) {
        aplicar(context, estaActivada(context))
    }

    private fun aplicar(context: Context, activada: Boolean) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(activada)
    }

    private fun preferencias(context: Context) =
        context.getSharedPreferences(FICHERO, Context.MODE_PRIVATE)
}
