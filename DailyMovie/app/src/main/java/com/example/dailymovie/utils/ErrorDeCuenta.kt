package com.example.dailymovie.utils

import androidx.annotation.StringRes
import com.example.dailymovie.R

/**
 * Por qué no se ha podido entrar, registrarse o tocar la cuenta.
 *
 * Antes esto era una cadena en castellano fabricada en el repositorio comparando el
 * `error.message` de Firebase contra siete frases **en inglés** (`"password is invalid"`,
 * `"badly formatted"`…). Dos problemas: en cuanto Firebase cambiara una redacción todo caía en
 * el genérico "algo ha fallado", y los mensajes vivían en la capa de datos, así que no se
 * podían traducir ni probar sin comparar textos.
 *
 * Ahora se mira el `errorCode` de `FirebaseAuthException`, que es una constante estable, y el
 * texto lo elige la pantalla. Es el mismo reparto que ya hace [com.example.dailymovie.utils.ErrorCarga]
 * con los fallos de red: **el ViewModel dice qué pasó, la vista dice cómo se cuenta.**
 */
enum class ErrorDeCuenta {
    /** El correo o la contraseña no cuadran. Firebase ya no distingue cuál de los dos. */
    CREDENCIALES,

    /** Ese correo ya tiene cuenta. */
    CORREO_COGIDO,

    /** Lo escrito no parece un correo. */
    CORREO_MAL_ESCRITO,

    /** La contraseña es demasiado corta para Firebase (mínimo seis). */
    CONTRASENA_CORTA,

    /** No hay ninguna cuenta con ese correo. */
    NO_EXISTE,

    /** No se pudo ni intentar: sin cobertura. */
    SIN_CONEXION,

    /**
     * Hace falta haber entrado hace poco.
     *
     * Firebase lo pide para lo delicado (borrar la cuenta, cambiar la contraseña) cuando la
     * sesión lleva mucho abierta.
     */
    SESION_VIEJA,

    /** Cualquier otra cosa. */
    DESCONOCIDO
}

/**
 * El texto que se le enseña al usuario.
 *
 * Vive aquí y no en el enum para que la capa de datos no toque recursos de Android y se pueda
 * probar sola, igual que con `ErrorCarga.mensaje()`.
 */
@StringRes
fun ErrorDeCuenta.mensaje(): Int = when (this) {
    ErrorDeCuenta.CREDENCIALES -> R.string.error_credenciales
    ErrorDeCuenta.CORREO_COGIDO -> R.string.error_correo_cogido
    ErrorDeCuenta.CORREO_MAL_ESCRITO -> R.string.error_correo_mal_escrito
    ErrorDeCuenta.CONTRASENA_CORTA -> R.string.error_contrasena_corta
    ErrorDeCuenta.NO_EXISTE -> R.string.error_cuenta_no_existe
    ErrorDeCuenta.SIN_CONEXION -> R.string.error_sin_conexion_cuenta
    ErrorDeCuenta.SESION_VIEJA -> R.string.error_sesion_vieja
    ErrorDeCuenta.DESCONOCIDO -> R.string.error_desconocido
}
