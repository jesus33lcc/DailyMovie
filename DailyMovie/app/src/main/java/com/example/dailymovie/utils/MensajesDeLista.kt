package com.example.dailymovie.utils

import androidx.annotation.StringRes
import com.example.dailymovie.R
import com.example.dailymovie.data.AltaDeLista

/**
 * Texto que se le enseña al usuario segun como haya ido crear la lista.
 *
 * Va aparte, igual que el de ErrorCarga, para que la capa de datos no tenga que saber nada
 * de los recursos de Android y se la pueda probar sola.
 */
@StringRes
fun AltaDeLista.mensaje(): Int = when (this) {
    AltaDeLista.CREADA -> R.string.lista_creada
    AltaDeLista.NOMBRE_VACIO -> R.string.lista_nombre_vacio
    AltaDeLista.NOMBRE_RESERVADO -> R.string.lista_nombre_reservado
    AltaDeLista.NOMBRE_INVALIDO -> R.string.lista_nombre_invalido
    AltaDeLista.YA_EXISTE -> R.string.lista_ya_existe
    AltaDeLista.SIN_SESION -> R.string.lista_sin_sesion
    AltaDeLista.RECHAZADA -> R.string.lista_rechazada
}
