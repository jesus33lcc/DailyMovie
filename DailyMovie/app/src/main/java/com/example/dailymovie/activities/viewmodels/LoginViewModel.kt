package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository

class LoginViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    /** Null mientras no se haya intentado nada, para no reaccionar al entrar en la pantalla. */
    private val _sesionIniciada = MutableLiveData<Boolean?>()
    val sesionIniciada: LiveData<Boolean?> get() = _sesionIniciada

    /**
     * Que avisarle al usuario, ya como recurso de texto.
     *
     * Es un @StringRes y no una cadena porque el texto lo elige la vista: aqui solo se dice
     * que ha pasado. Antes el repositorio devolvia castellano escrito a pelo.
     */
    private val _mensaje = MutableLiveData<Int?>()
    val mensaje: LiveData<Int?> get() = _mensaje

    private val _correoDeRecuperacionEnviado = MutableLiveData<Boolean?>()
    val correoDeRecuperacionEnviado: LiveData<Boolean?> get() = _correoDeRecuperacionEnviado

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    fun entrar(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _mensaje.value = R.string.error_campos_vacios
            return
        }
        _cargando.value = true
        usuario.entrar(correo.trim(), contrasena) { bien, error ->
            _cargando.value = false
            if (bien) {
                _sesionIniciada.value = true
            } else {
                _mensaje.value = error?.mensaje() ?: R.string.error_desconocido
            }
        }
    }

    /** El token lo consigue la vista con el dialogo de Google; aqui solo se canjea. */
    fun entrarConGoogle(idToken: String) {
        _cargando.value = true
        usuario.entrarConGoogle(idToken) { bien, error ->
            _cargando.value = false
            if (bien) {
                _sesionIniciada.value = true
            } else {
                _mensaje.value = error?.mensaje() ?: R.string.error_desconocido
            }
        }
    }

    fun recuperarContrasena(correo: String) {
        _cargando.value = true
        usuario.mandarCorreoDeRecuperacion(correo.trim()) { bien, error ->
            _cargando.value = false
            if (bien) {
                _correoDeRecuperacionEnviado.value = true
            } else {
                _mensaje.value = error?.mensaje() ?: R.string.error_desconocido
            }
        }
    }

    fun haySesion() = usuario.haySesion()

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    fun recuperacionMostrada() {
        _correoDeRecuperacionEnviado.value = null
    }
}
