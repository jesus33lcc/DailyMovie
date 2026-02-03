package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository

class SettingsViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    /** Que avisarle al usuario, ya como recurso de texto. El texto lo elige la vista. */
    private val _mensaje = MutableLiveData<Int?>()
    val mensaje: LiveData<Int?> get() = _mensaje

    /** Se pone a true cuando ya no hay sesion y la vista tiene que salir a Login. */
    private val _sesionCerrada = MutableLiveData<Boolean?>()
    val sesionCerrada: LiveData<Boolean?> get() = _sesionCerrada

    fun correoDelUsuario() = usuario.correoDelUsuario()

    /** Los usuarios de Google no tienen contraseña que cambiar ni que confirmar. */
    fun entroConCorreo() = usuario.correoDelUsuario() != null

    fun borrarHistorial(alTerminar: (Boolean) -> Unit) = usuario.borrarHistorial(alTerminar)

    fun cerrarSesion() {
        usuario.salir { _sesionCerrada.value = true }
    }

    fun cambiarContrasena(actual: String, nueva: String) {
        usuario.cambiarContrasena(actual, nueva) { bien, motivo ->
            _mensaje.value =
                if (bien) R.string.ajustes_contrasena_cambiada
                else motivo?.mensaje() ?: R.string.error_desconocido
        }
    }

    /**
     * Borra los datos y la cuenta. Play Store lo exige en cualquier app con registro.
     * La contraseña hace falta porque Firebase pide identificarse otra vez antes de borrar.
     */
    fun borrarCuenta(contrasena: String?) {
        usuario.borrarCuenta(contrasena) { bien, motivo ->
            _mensaje.value =
                if (bien) R.string.ajustes_cuenta_borrada
                else motivo?.mensaje() ?: R.string.error_desconocido
            if (bien) _sesionCerrada.value = true
        }
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
