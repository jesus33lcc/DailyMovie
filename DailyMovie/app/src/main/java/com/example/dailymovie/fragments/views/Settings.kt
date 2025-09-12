package com.example.dailymovie.fragments.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.dailymovie.R
import com.example.dailymovie.activities.views.LoginA
import com.example.dailymovie.activities.views.OnboardingA
import com.example.dailymovie.databinding.FragmentSettingsBinding
import com.example.dailymovie.fragments.viewmodels.SettingsViewModel
import com.example.dailymovie.utils.Analitica
import com.example.dailymovie.utils.LocaleUtil

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
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
        AlertDialog.Builder(requireContext())
            .setTitle("Región e idioma")
            .setMessage(
                "Ahora mismo: ${LocaleUtil.getDeviceCountry()} · ${LocaleUtil.getDeviceLanguage()}\n\n" +
                    "Con esto decidimos en qué idioma te enseñamos las películas y qué " +
                    "plataformas de streaming te salen, porque no son las mismas en cada país.\n\n" +
                    "Se coge de los ajustes del móvil. Si lo cambias ahí, la app te sigue."
            )
            .setPositiveButton("Abrir ajustes del móvil") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS))
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun confirmarBorrarHistorial() {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar historial")
            .setMessage("Se borra todo lo que has buscado. Tus listas y favoritos no se tocan.")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.borrarHistorial { bien ->
                    val texto = if (bien) "Historial borrado" else "No se ha podido borrar"
                    Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarCambiarContrasena() {
        if (!viewModel.entroConCorreo()) {
            Toast.makeText(
                requireContext(),
                "Entraste con Google, así que la contraseña se cambia desde tu cuenta de Google",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val actual = vista.findViewById<EditText>(R.id.editTextCurrentPassword)
        val nueva = vista.findViewById<EditText>(R.id.editTextNewPassword)
        val repetida = vista.findViewById<EditText>(R.id.editTextConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar contraseña")
            .setView(vista)
            .setPositiveButton("Cambiar") { _, _ ->
                val laActual = actual.text.toString()
                val laNueva = nueva.text.toString()
                when {
                    laActual.isEmpty() || laNueva.isEmpty() ->
                        avisar("Rellena las dos contraseñas")
                    laNueva != repetida.text.toString() ->
                        avisar("Las dos contraseñas nuevas no coinciden")
                    laNueva.length < 6 ->
                        avisar("La contraseña necesita al menos 6 caracteres")
                    else -> viewModel.cambiarContrasena(laActual, laNueva)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro? Tus listas se quedan guardadas para cuando vuelvas.")
            .setPositiveButton("Cerrar sesión") { _, _ -> viewModel.cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirPoliticaDePrivacidad() {
        // Se abre fuera para que el texto se pueda actualizar sin sacar version nueva.
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_PRIVACIDAD)))
    }

    /**
     * Consentimiento de analitica. Google Play obliga a pedirlo y a poder retirarlo, y
     * ademas hay que declararlo en el formulario de Seguridad de los datos.
     */
    private fun mostrarConsentimiento() {
        val activada = Analitica.estaActivada(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Datos de uso")
            .setMessage(
                "Recogemos datos anónimos de cómo se usa la app (qué pantallas se abren, " +
                    "si algo falla) para saber qué mejorar.\n\n" +
                    "No incluye lo que ves ni tus listas, y puedes desactivarlo cuando quieras.\n\n" +
                    "Ahora mismo está ${if (activada) "activado" else "desactivado"}."
            )
            .setPositiveButton(if (activada) "Desactivar" else "Activar") { _, _ ->
                Analitica.activar(requireContext(), !activada)
                val texto = if (activada) "Datos de uso desactivados" else "Datos de uso activados"
                Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /**
     * Borrado de cuenta. Google Play lo exige en cualquier app con registro, y tiene que
     * borrar de verdad los datos, no solo cerrar sesion.
     */
    private fun confirmarBorrarCuenta() {
        AlertDialog.Builder(requireContext())
            .setTitle("Borrar mi cuenta")
            .setMessage(
                "Se borra tu cuenta y todo lo que tenemos guardado: favoritos, vistos, " +
                    "listas, historial y tus gustos.\n\n" +
                    "Esto no se puede deshacer."
            )
            .setPositiveButton("Continuar") { _, _ -> pedirContrasenaYBorrar() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pedirContrasenaYBorrar() {
        if (!viewModel.entroConCorreo()) {
            // Con Google no hay contraseña que pedir: vale la sesion reciente.
            viewModel.borrarCuenta(null)
            return
        }

        val campo = EditText(requireContext()).apply {
            hint = "Tu contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirma que eres tú")
            .setMessage("Escribe tu contraseña para poder borrar la cuenta.")
            .setView(campo)
            .setPositiveButton("Borrar definitivamente") { _, _ ->
                val contrasena = campo.text.toString()
                if (contrasena.isEmpty()) {
                    avisar("Escribe tu contraseña")
                } else {
                    viewModel.borrarCuenta(contrasena)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun irALogin() {
        val intent = Intent(requireContext(), LoginA::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun avisar(texto: String) {
        Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val URL_PRIVACIDAD = "https://github.com/jesus33lcc/DailyMovie"
    }
}
