package com.example.dailymovie.client

import com.example.dailymovie.data.Resultado
import com.example.dailymovie.utils.ErrorCarga
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Lanza la peticion y reparte el resultado por dos caminos: datos correctos o fallo.
 *
 * Nace de dos problemas que se repetian en todas las llamadas: el bloque de Retrofit
 * estaba copiado y pegado una y otra vez, y los `onFailure` se quedaban vacios, asi que
 * si se caia la red la pantalla se quedaba en blanco sin explicar nada. Con esto cada
 * fallo tiene salida, incluido el caso de que el servidor conteste con un codigo de error.
 *
 * Retrofit avisa en el hilo principal, asi que desde `onExito` se puede tocar LiveData
 * con `.value` sin problemas.
 *
 * @param onExito recibe el cuerpo de la respuesta ya convertido. Solo se llama si el servidor
 *   contesto bien y ademas vino cuerpo: si TMDB devuelve un 200 vacio, esto no salta.
 * @param onError recibe por que no hay datos: [ErrorCarga.SIN_CONEXION] si la peticion ni
 *   siquiera salio, y [ErrorCarga.RESPUESTA_INVALIDA] si contesto pero con un codigo de error
 *   o sin cuerpo. Uno de los dos bloques se llama siempre, nunca los dos.
 */
fun <T> Call<T>.enqueueSimple(
    onExito: (T) -> Unit,
    onError: (ErrorCarga) -> Unit
) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val cuerpo = response.body()
            if (response.isSuccessful && cuerpo != null) {
                onExito(cuerpo)
            } else {
                onError(ErrorCarga.RESPUESTA_INVALIDA)
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            onError(ErrorCarga.SIN_CONEXION)
        }
    })
}

/**
 * Lo mismo que [enqueueSimple] pero devolviendo ya un [Resultado].
 *
 * Es lo que hacen casi todos los repositorios: envolver el cuerpo en `Resultado.Exito` y el
 * motivo en `Resultado.Fallo`. Estaba escrito veinticuatro veces exactamente igual, y las
 * cuatro lineas del `onError` no cambiaban ni una coma.
 *
 * @param alTerminar recibe los datos o el motivo por el que no hay ninguno.
 */
fun <T : Any> Call<T>.enqueueResultado(alTerminar: (Resultado<T>) -> Unit) {
    enqueueSimple(
        onExito = { alTerminar(Resultado.Exito(it)) },
        onError = { alTerminar(Resultado.Fallo(it)) }
    )
}

/**
 * Como [enqueueResultado], pero pasando antes la respuesta por una conversion.
 *
 * Casi ninguna pantalla quiere el sobre de TMDB tal cual: quiere la lista de dentro, o el
 * modelo de dominio. Con esto la conversion se escribe en una linea y el reparto de errores
 * sigue siendo el mismo de siempre.
 *
 * @param convertir de la respuesta de TMDB a lo que de verdad se usa arriba.
 * @param alTerminar recibe lo ya convertido, o el motivo del fallo.
 */
fun <T : Any, R : Any> Call<T>.enqueueConvertido(
    convertir: (T) -> R,
    alTerminar: (Resultado<R>) -> Unit
) {
    enqueueSimple(
        onExito = { alTerminar(Resultado.Exito(convertir(it))) },
        onError = { alTerminar(Resultado.Fallo(it)) }
    )
}
