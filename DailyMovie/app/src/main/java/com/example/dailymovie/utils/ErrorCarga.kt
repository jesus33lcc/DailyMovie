package com.example.dailymovie.utils

import androidx.annotation.StringRes
import com.example.dailymovie.R

/**
 * Motivo por el que no se han podido cargar unos datos.
 *
 * El ViewModel solo dice QUE ha fallado; el texto que ve el usuario lo elige la vista.
 * Asi los ViewModels no necesitan acceso a los recursos de Android y se pueden probar solos.
 */
enum class ErrorCarga {

    /** No hubo respuesta: sin internet, servidor caido o se agoto el tiempo de espera. */
    SIN_CONEXION,

    /** El servidor contesto, pero con un codigo de error o sin contenido. */
    RESPUESTA_INVALIDA
}

/** Texto que se le ensena al usuario para cada tipo de fallo. */
@StringRes
fun ErrorCarga.mensaje(): Int = when (this) {
    ErrorCarga.SIN_CONEXION -> R.string.error_sin_conexion
    ErrorCarga.RESPUESTA_INVALIDA -> R.string.error_respuesta_invalida
}
