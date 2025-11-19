package com.example.dailymovie.utils

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * El botoncito que da un salto al pulsarlo.
 *
 * Marcar una pelicula como favorita no cambia nada en pantalla salvo el dibujo del corazon, y
 * ese cambio es tan pequeño que se pierde: mas de una vez se toca dos veces sin querer porque
 * no parece que haya pasado nada. Con el rebote se nota, y ademas se nota "fisico" y no como
 * una animacion enlatada de medio segundo.
 *
 * Se usa un muelle y no un `scale` de duracion fija porque si el usuario pulsa varias veces
 * seguidas el muelle recoge el estado en el que va y sigue desde ahi, en vez de dar un tiron
 * al reiniciarse.
 */
fun View.rebotar() {
    scaleX = ESCALA_AL_PULSAR
    scaleY = ESCALA_AL_PULSAR
    muelle(DynamicAnimation.SCALE_X).start()
    muelle(DynamicAnimation.SCALE_Y).start()
}

private fun View.muelle(propiedad: DynamicAnimation.ViewProperty) =
    SpringAnimation(this, propiedad, 1f).apply {
        spring.stiffness = SpringForce.STIFFNESS_MEDIUM
        // Poco amortiguamiento: rebota un par de veces antes de asentarse. Con el valor por
        // defecto se queda en un crecimiento suave que no se ve.
        spring.dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
    }

/** Se encoge primero para que el rebote tenga de donde salir. */
private const val ESCALA_AL_PULSAR = 0.75f
