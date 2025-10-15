package com.example.dailymovie.fragments.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dailymovie.R
import com.example.dailymovie.activities.views.LoginA
import com.example.dailymovie.activities.views.OnboardingA
import com.example.dailymovie.databinding.FragmentSettingsBinding
import com.example.dailymovie.fragments.viewmodels.SettingsViewModel
import com.example.dailymovie.utils.Analitica
import com.example.dailymovie.utils.LocaleUtil
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.DialogoDailyMovie

class Settings : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtCorreoUsuario.text = viewModel.correoDelUsuario() ?: "Cuenta de Google"
        binding.txtVersion.text = "DailyMovie ${com.example.dailymovie.BuildConfig.VERSION_NAME}"

        binding.btnEditarGustos.setOnClickListener {
            startActivity(Intent(requireContext(), OnboardingA::class.java).apply {
                putExtra(OnboardingA.EXTRA_DESDE_AJUSTES, true)
            })
        }

        binding.btnRegion.setOnClickListener { mostrarRegion() }
        binding.btnClearHistory.setOnClickListener { confirmarBorrarHistorial() }
        binding.btnChangePassword.setOnClickListener { mostrarCambiarContrasena() }
        binding.btnLogout.setOnClickListener { confirmarCerrarSesion() }
        binding.btnPoliticaPrivacidad.setOnClickListener { abrirPoliticaDePrivacidad() }
        binding.btnConsentimientoAnalytics.setOnClickListener { mostrarConsentimiento() }
        binding.btnBorrarCuenta.setOnClickListener { confirmarBorrarCuenta() }

        viewModel.mensaje.observe(viewLifecycleOwner) { texto ->
            texto?.let {
                Avisos.breve(binding.root, it)
                viewModel.mensajeMostrado()
            }
        }

        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada == true) irALogin()
        }
    }

    /**
     * El boton de Region no hacia nada. Ahora al menos explica de donde sale y como
     * cambiarla, que es la verdad: TMDB usa el idioma y el pais del dispositivo, asi que
     * se cambia en los ajustes de Android y no dentro de la app.
     */
    private fun mostrarRegion() {
        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = "Región e idioma",
            mensaje = "Ahora mismo: ${LocaleUtil.getDeviceCountry()} · ${LocaleUtil.getDeviceLanguage()}\n\n" +
                "Con esto decidimos en qué idioma te enseñamos las películas y qué " +
                "plataformas de streaming te salen, porque no son las mismas en cada país.\n\n" +
                "Se coge de los ajustes del móvil. Si lo cambias ahí, la app te sigue.",
            textoAceptar = "Abrir ajustes del móvil",
            textoCancelar = "Cerrar"
        ) { dialogo ->
            dialogo.dismiss()
            startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS))
        }
    }

    private fun confirmarBorrarHistorial() {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = "Borrar historial",
            mensaje = "Se borra todo lo que has buscado. Tus listas y favoritos no se tocan.",
            textoAceptar = "Borrar",
            peligroso = true
        ) {
            viewModel.borrarHistorial { bien ->
                Avisos.breve(binding.root, if (bien) "Historial borrado" else "No se ha podido borrar")
            }
        }
    }

    private fun mostrarCambiarContrasena() {
        if (!viewModel.entroConCorreo()) {
            Avisos.largo(binding.root, "Entraste con Google, así que la contraseña se cambia desde tu cuenta de Google")
            return
        }

        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val actual = vista.findViewById<EditText>(R.id.editTextCurrentPassword)
        val nueva = vista.findViewById<EditText>(R.id.editTextNewPassword)
        val repetida = vista.findViewById<EditText>(R.id.editTextConfirmPassword)

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = "Cambiar contraseña",
            contenido = vista,
            textoAceptar = "Cambiar"
        ) { dialogo ->
            val laActual = actual.text.toString()
            val laNueva = nueva.text.toString()
            when {
                laActual.isEmpty() || laNueva.isEmpty() ->
                    avisar("Rellena las dos contraseñas")
                laNueva != repetida.text.toString() ->
                    avisar("Las dos contraseñas nuevas no coinciden")
                laNueva.length < 6 ->
                    avisar("La contraseña necesita al menos 6 caracteres")
                else -> {
                    dialogo.dismiss()
                    viewModel.cambiarContrasena(laActual, laNueva)
                }
            }
        }
    }

    private fun confirmarCerrarSesion() {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = "Cerrar sesión",
            mensaje = "¿Seguro? Tus listas se quedan guardadas para cuando vuelvas.",
            textoAceptar = "Cerrar sesión"
        ) {
            viewModel.cerrarSesion()
        }
    }

    private fun abrirPoliticaDePrivacidad() {
        // Se abre fuera para que el texto se pueda actualizar sin sacar version nueva.
        //
        // De momento lleva al repositorio, porque la politica todavia esta sin escribir.
        // Google Play la exige para publicar cualquier app con registro, asi que cuando
        // exista hay que cambiar la direccion de abajo y ya.
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACIDAD)))
    }

    /**
     * Consentimiento de analitica. Google Play obliga a pedirlo y a poder retirarlo, y
     * ademas hay que declararlo en el formulario de Seguridad de los datos.
     */
    private fun mostrarConsentimiento() {
        val activada = Analitica.estaActivada(requireContext())
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = "Datos de uso",
            mensaje = "Recogemos datos anónimos de cómo se usa la app (qué pantallas se abren, " +
                "si algo falla) para saber qué mejorar.\n\n" +
                "No incluye lo que ves ni tus listas, y puedes desactivarlo cuando quieras.\n\n" +
                "Ahora mismo está ${if (activada) "activado" else "desactivado"}.",
            textoAceptar = if (activada) "Desactivar" else "Activar"
        ) {
            Analitica.activar(requireContext(), !activada)
            val texto = if (activada) "Datos de uso desactivados" else "Datos de uso activados"
            Avisos.breve(binding.root, texto)
        }
    }

    /**
     * Borrado de cuenta. Google Play lo exige en cualquier app con registro, y tiene que
     * borrar de verdad los datos, no solo cerrar sesion.
     */
    private fun confirmarBorrarCuenta() {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = "Borrar mi cuenta",
            mensaje = "Se borra tu cuenta y todo lo que tenemos guardado: favoritos, vistos, " +
                "listas, historial y tus gustos.\n\n" +
                "Esto no se puede deshacer.",
            textoAceptar = "Continuar",
            peligroso = true
        ) {
            pedirContrasenaYBorrar()
        }
    }

    private fun pedirContrasenaYBorrar() {
        if (!viewModel.entroConCorreo()) {
            // Con Google no hay contraseña que pedir: vale la sesion reciente.
            viewModel.borrarCuenta(null)
            return
        }

        // El mismo campo que usa el dialogo de crear lista, para no montar otro a mano.
        val contenido = layoutInflater.inflate(R.layout.dialogo_campo_texto, null)
        val campo = contenido.findViewById<EditText>(R.id.dialogoCampo).apply {
            hint = "Tu contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = "Confirma que eres tú",
            mensaje = "Escribe tu contraseña para poder borrar la cuenta.",
            contenido = contenido,
            textoAceptar = "Borrar definitivamente",
            peligroso = true
        ) { dialogo ->
            val contrasena = campo.text.toString()
            if (contrasena.isEmpty()) {
                // Se deja abierto: cerrarlo obligaria a empezar de cero.
                contenido.findViewById<TextView>(R.id.dialogoAviso).apply {
                    text = "Escribe tu contraseña"
                    visibility = View.VISIBLE
                }
            } else {
                dialogo.dismiss()
                viewModel.borrarCuenta(contrasena)
            }
        }
    }

    private fun irALogin() {
        val intent = Intent(requireContext(), LoginA::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun avisar(texto: String) {
        Avisos.breve(binding.root, texto)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val URL_PRIVACIDAD = "https://github.com/jesus33lcc/DailyMovie"
    }
}
