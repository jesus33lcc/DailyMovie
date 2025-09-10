package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository

class RegistroViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _registrado = MutableLiveData<Boolean?>()
    val registrado: LiveData<Boolean?> get() = _registrado

    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> get() = _mensaje

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    fun registrar(correo: String, contrasena: String, repetida: String) {
        // Se comprueba aqui y no en la vista para que la pantalla solo se ocupe de pintar.
        when {
            correo.isBlank() || contrasena.isBlank() ->
                _mensaje.value = "Rellena el correo y la contraseña"
            contrasena != repetida ->
                _mensaje.value = "Las dos contraseñas no coinciden"
            contrasena.length < 6 ->
                _mensaje.value = "La contraseña necesita al menos 6 caracteres"
            else -> {
                _cargando.value = true
                usuario.registrar(correo.trim(), contrasena) { bien, error ->
                    _cargando.value = false
                    if (bien) {
                        _registrado.value = true
                    } else {
                        _mensaje.value = error
                    }
                }
            }
        }
    }

    fun entrarConGoogle(idToken: String) {
        _cargando.value = true
        usuario.entrarConGoogle(idToken) { bien, error ->
            _cargando.value = false
            if (bien) {
                _registrado.value = true
            } else {
                _mensaje.value = error
            }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
