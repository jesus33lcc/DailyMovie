package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository

class LoginViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    /** Null mientras no se haya intentado nada, para no reaccionar al entrar en la pantalla. */
    private val _sesionIniciada = MutableLiveData<Boolean?>()
    val sesionIniciada: LiveData<Boolean?> get() = _sesionIniciada

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> get() = _mensaje

    private val _correoDeRecuperacionEnviado = MutableLiveData<Boolean?>()
    val correoDeRecuperacionEnviado: LiveData<Boolean?> get() = _correoDeRecuperacionEnviado

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    fun entrar(correo: String, contrasena: String) {
        if (correo.isBlank() || contrasena.isBlank()) {
            _mensaje.value = "Rellena el correo y la contraseña"
            return
        }
        _cargando.value = true
        usuario.entrar(correo.trim(), contrasena) { bien, error ->
            _cargando.value = false
            if (bien) {
                _sesionIniciada.value = true
            } else {
                _mensaje.value = error
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
                _mensaje.value = error
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
                _mensaje.value = error
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
