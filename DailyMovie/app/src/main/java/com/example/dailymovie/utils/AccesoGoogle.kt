package com.example.dailymovie.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.dailymovie.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * El paso previo al login con Google: conseguir el token que despues canjea Firebase.
 *
 * Se saca aqui porque lo usan Login y Registro igual, y para que las pantallas no tengan
 * que saber nada de la API de Google.
 *
 * OJO: Google valida la firma del APK contra los SHA-1 registrados en Firebase. Si falla con
 * el codigo 10 (DEVELOPER_ERROR) no es un fallo del codigo, es que falta dar de alta la
 * huella de ese build en la consola.
 */
object AccesoGoogle {

    private const val CODIGO_DESARROLLADOR_MAL = 10

    /**
     * El cliente con el que se abre el dialogo de "elige una cuenta".
     *
     * @param context el de la pantalla desde la que se va a entrar.
     * @return el cliente ya configurado para pedir el token de id y el correo, que es lo
     *   unico que hace falta para despues entrar en Firebase.
     */
    fun cliente(context: Context): GoogleSignInClient {
        // default_web_client_id lo genera el plugin de google-services leyendo el cliente
        // de tipo 3 de google-services.json, asi que no hay que copiar ningun id a mano.
        val opciones = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, opciones)
    }

    /**
     * Saca el token del resultado del dialogo de Google.
     * Devuelve el token si todo fue bien, o null y el motivo si no.
     *
     * @param datos lo que trae de vuelta el dialogo. Puede ser null si se cerro sin elegir.
     * @param alTerminar recibe (token, null) si salio bien y (null, motivo) si no. El motivo
     *   viene ya escrito para el usuario, asi que la pantalla lo puede enseñar tal cual.
     */
    fun tokenDelResultado(datos: Intent?, alTerminar: (String?, String?) -> Unit) {
        try {
            val cuenta = GoogleSignIn.getSignedInAccountFromIntent(datos).getResult(ApiException::class.java)
            val token = cuenta.idToken
            if (token == null) {
                alTerminar(null, "Google no ha devuelto el token de acceso")
            } else {
                alTerminar(token, null)
            }
        } catch (e: ApiException) {
            alTerminar(null, explicar(e))
        }
    }

    /**
     * Cierra tambien la sesion de Google, para que la proxima vez vuelva a preguntar.
     *
     * @param activity la pantalla desde la que se cierra sesion.
     */
    fun olvidarCuenta(activity: Activity) {
        cliente(activity).signOut()
    }

    private fun explicar(e: ApiException): String = when (e.statusCode) {
        CODIGO_DESARROLLADOR_MAL ->
            "Falta registrar la huella SHA-1 de esta versión en Firebase"
        com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR ->
            "No hay conexión. Revisa tu internet"
        com.google.android.gms.common.api.CommonStatusCodes.CANCELED ->
            "Has cancelado el inicio de sesión"
        else -> "No se ha podido entrar con Google"
    }
}
