package com.example.dailymovie.client

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
