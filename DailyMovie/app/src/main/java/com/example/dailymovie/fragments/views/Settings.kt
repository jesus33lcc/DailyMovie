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
import com.example.dailymovie.activities.views.EstadisticasA
import com.example.dailymovie.activities.views.OnboardingA
import com.example.dailymovie.databinding.FragmentSettingsBinding
import com.example.dailymovie.fragments.viewmodels.SettingsViewModel
import com.example.dailymovie.utils.Analitica
import com.example.dailymovie.utils.LocaleUtil
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.dailymovie.utils.Idioma
import com.example.dailymovie.utils.IdiomaDelContenido
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.siLaVistaSigueAhi
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

        binding.txtCorreoUsuario.text =
            viewModel.correoDelUsuario() ?: getString(R.string.ajustes_cuenta_google)
        // La GPL pide que el aviso se vea desde la propia app, no solo en el repositorio.
        binding.txtVersion.text =
            getString(R.string.ajustes_version, com.example.dailymovie.BuildConfig.VERSION_NAME)

        binding.btnEditarGustos.setOnClickListener {
            startActivity(Intent(requireContext(), OnboardingA::class.java).apply {
                putExtra(OnboardingA.EXTRA_DESDE_AJUSTES, true)
            })
        }

        binding.btnMisNumeros.setOnClickListener {
            startActivity(Intent(requireContext(), EstadisticasA::class.java))
        }

        binding.btnIdioma.setOnClickListener { mostrarIdiomas() }
        binding.btnRegion.setOnClickListener { mostrarRegion() }
        binding.btnClearHistory.setOnClickListener { confirmarBorrarHistorial() }
        binding.btnChangePassword.setOnClickListener { mostrarCambiarContrasena() }
        binding.btnLogout.setOnClickListener { confirmarCerrarSesion() }
        binding.btnPoliticaPrivacidad.setOnClickListener { abrirPoliticaDePrivacidad() }
        binding.btnConsentimientoAnalytics.setOnClickListener { mostrarConsentimiento() }
        binding.btnBorrarCuenta.setOnClickListener { confirmarBorrarCuenta() }

        viewModel.mensaje.observe(viewLifecycleOwner) { texto ->
            texto?.let {
                avisar(it)
                viewModel.mensajeMostrado()
            }
        }

        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada == true) irALogin()
        }
    }

    /**
     * Elegir en que idioma llegan las peliculas.
     *
     * Se avisa de que los textos de la app no cambian: TMDB devuelve titulos y sinopsis en el
     * idioma que se le pida, pero los botones y los avisos estan escritos en español. Sin
     * decirlo, elegir "English" y ver la app medio en español parece un fallo.
     *
     * Al cambiarlo se rehace la pantalla: lo que ya estaba cargado se pidio en el idioma de
     * antes y no se entera de nada.
     */
    private fun mostrarIdiomas() {
        val contenido = layoutInflater.inflate(R.layout.dialogo_opciones, null)
        val grupo = contenido.findViewById<RadioGroup>(R.id.dialogoOpciones)
        val puesto = IdiomaDelContenido.elPuesto(requireContext())
        var elegido = puesto

        Idioma.entries.forEach { idioma ->
            val opcion = layoutInflater.inflate(R.layout.item_opcion_radio, grupo, false) as RadioButton
            opcion.id = View.generateViewId()
            opcion.text = idioma.nombre
            opcion.isChecked = idioma == puesto
            opcion.setOnClickListener { elegido = idioma }
            grupo.addView(opcion)
        }

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = getString(R.string.ajustes_idioma_titulo),
            mensaje = getString(R.string.ajustes_idioma_mensaje),
            contenido = contenido,
            textoAceptar = getString(R.string.boton_aplicar)
        ) { dialogo ->
            dialogo.dismiss()
            if (elegido == puesto) return@mostrar
            IdiomaDelContenido.cambiar(requireContext(), elegido)
            // Lo que ya esta en pantalla se pidio en el idioma de antes, asi que se rehace la
            // pantalla entera: la portada, las listas y las fichas se vuelven a pedir solas.
            requireActivity().recreate()
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
            titulo = getString(R.string.ajustes_region_titulo),
            mensaje = getString(
                R.string.ajustes_region_mensaje,
                LocaleUtil.getDeviceCountry(),
                LocaleUtil.getDeviceLanguage()
            ),
            textoAceptar = getString(R.string.ajustes_region_aceptar),
            textoCancelar = getString(R.string.boton_cerrar)
        ) { dialogo ->
            dialogo.dismiss()
            startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS))
        }
    }

    private fun confirmarBorrarHistorial() {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = getString(R.string.ajustes_borrar_historial_titulo),
            mensaje = getString(R.string.ajustes_borrar_historial_mensaje),
            textoAceptar = getString(R.string.boton_borrar),
            peligroso = true
        ) {
            viewModel.borrarHistorial { bien ->
                _binding.siLaVistaSigueAhi { vista ->
                    Avisos.breve(
                        vista.root,
                        getString(
                            if (bien) R.string.aviso_historial_borrado
                            else R.string.aviso_historial_no_borrado
                        )
                    )
                }
            }
        }
    }

    private fun mostrarCambiarContrasena() {
        if (!viewModel.entroConCorreo()) {
            Avisos.largo(binding.root, getString(R.string.ajustes_contrasena_con_google))
            return
        }

        val vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val actual = vista.findViewById<EditText>(R.id.editTextCurrentPassword)
        val nueva = vista.findViewById<EditText>(R.id.editTextNewPassword)
        val repetida = vista.findViewById<EditText>(R.id.editTextConfirmPassword)

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = getString(R.string.ajustes_cambiar_contrasena_titulo),
            contenido = vista,
            textoAceptar = getString(R.string.boton_cambiar)
        ) { dialogo ->
            val laActual = actual.text.toString()
            val laNueva = nueva.text.toString()
            when {
                laActual.isEmpty() || laNueva.isEmpty() ->
                    avisar(getString(R.string.ajustes_contrasena_rellena))
                laNueva != repetida.text.toString() ->
                    avisar(getString(R.string.ajustes_contrasena_no_coinciden))
                laNueva.length < 6 ->
                    avisar(getString(R.string.ajustes_contrasena_corta))
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
            titulo = getString(R.string.ajustes_cerrar_sesion),
            mensaje = getString(R.string.ajustes_cerrar_sesion_mensaje),
            textoAceptar = getString(R.string.ajustes_cerrar_sesion)
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
            titulo = getString(R.string.ajustes_datos_uso_titulo),
            mensaje = getString(
                R.string.ajustes_datos_uso_mensaje,
                getString(
                    if (activada) R.string.ajustes_datos_uso_activado
                    else R.string.ajustes_datos_uso_desactivado
                )
            ),
            textoAceptar = getString(
                if (activada) R.string.boton_desactivar else R.string.boton_activar
            )
        ) {
            Analitica.activar(requireContext(), !activada)
            avisar(
                getString(
                    if (activada) R.string.aviso_datos_uso_desactivados
                    else R.string.aviso_datos_uso_activados
                )
            )
        }
    }

    /**
     * Borrado de cuenta. Google Play lo exige en cualquier app con registro, y tiene que
     * borrar de verdad los datos, no solo cerrar sesion.
     */
    private fun confirmarBorrarCuenta() {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = getString(R.string.ajustes_borrar_cuenta_titulo),
            mensaje = getString(R.string.ajustes_borrar_cuenta_mensaje),
            textoAceptar = getString(R.string.boton_continuar),
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
            hint = getString(R.string.ajustes_hint_contrasena)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = getString(R.string.ajustes_confirmar_identidad_titulo),
            mensaje = getString(R.string.ajustes_confirmar_identidad_mensaje),
            contenido = contenido,
            textoAceptar = getString(R.string.ajustes_borrar_definitivamente),
            peligroso = true
        ) { dialogo ->
            val contrasena = campo.text.toString()
            if (contrasena.isEmpty()) {
                // Se deja abierto: cerrarlo obligaria a empezar de cero.
                contenido.findViewById<TextView>(R.id.dialogoAviso).apply {
                    setText(R.string.ajustes_escribe_contrasena)
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

    /**
     * Un aviso, si la pantalla sigue ahi para enseñarlo.
     *
     * Lo llaman callbacks de Firebase que pueden tardar (cambiar contraseña, borrar cuenta),
     * y para entonces el usuario puede haberse ido de la pestaña.
     */
    private fun avisar(texto: String) {
        _binding.siLaVistaSigueAhi { vista -> Avisos.breve(vista.root, texto) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val URL_PRIVACIDAD = "https://github.com/jesus33lcc/DailyMovie"
    }
}
