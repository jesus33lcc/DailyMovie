package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository

class RegistroViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _registrado = MutableLiveData<Boolean?>()
    val registrado: LiveData<Boolean?> get() = _registrado

    /**
     * Que avisarle al usuario, ya como recurso de texto.
     *
     * Es un @StringRes y no una cadena porque el texto lo elige la vista: aqui solo se dice
     * que ha pasado. Antes el repositorio devolvia castellano escrito a pelo.
     */
    private val _mensaje = MutableLiveData<Int?>()
    val mensaje: LiveData<Int?> get() = _mensaje

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    fun registrar(correo: String, contrasena: String, repetida: String) {
        // Se comprueba aqui y no en la vista para que la pantalla solo se ocupe de pintar.
        when {
            correo.isBlank() || contrasena.isBlank() ->
                _mensaje.value = R.string.error_campos_vacios
            contrasena != repetida ->
                _mensaje.value = R.string.error_contrasenas_distintas
            contrasena.length < 6 ->
                _mensaje.value = R.string.error_contrasena_corta
            else -> {
                _cargando.value = true
                usuario.registrar(correo.trim(), contrasena) { bien, error ->
                    _cargando.value = false
                    if (bien) {
                        _registrado.value = true
                    } else {
                        _mensaje.value = error?.mensaje() ?: R.string.error_desconocido
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
                _mensaje.value = error?.mensaje() ?: R.string.error_desconocido
            }
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
