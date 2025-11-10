package com.example.dailymovie.utils

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.app.ActivityOptionsCompat

/**
 * Abrir una ficha haciendo que el cartel viaje desde la tarjeta.
 *
 * Sin esto, al tocar un cartel la pantalla nueva subia entera desde abajo y el cartel volvia
 * a aparecer de la nada en otro sitio. Con la transicion compartida, el cartel que ya se
 * estaba mirando crece hasta su hueco en la ficha, que es lo que el ojo espera.
 *
 * @param destino la pantalla que se abre, con sus extras ya puestos.
 * @param cartel la imagen de la tarjeta que se ha tocado. Tiene que llevar puesto el mismo
 *   `transitionName` que la imagen de la pantalla de destino, o la animacion no ocurre.
 * @param nombre ese `transitionName` compartido.
 */
fun Activity.abrirConCartel(destino: Intent, cartel: View, nombre: String) {
    val opciones = ActivityOptionsCompat.makeSceneTransitionAnimation(this, cartel, nombre)
    startActivity(destino, opciones.toBundle())
}
