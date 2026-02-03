package com.example.dailymovie.activities.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dailymovie.activities.viewmodels.LoginViewModel
import com.example.dailymovie.graphics.mostrarSi
import com.example.dailymovie.databinding.ActivityLoginBinding
import com.example.dailymovie.utils.AccesoGoogle
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.R

class LoginA : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    /** Recoge lo que devuelve el dialogo de cuentas de Google. */
    private val dialogoDeGoogle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        AccesoGoogle.tokenDelResultado(resultado.data) { token, error ->
            if (token != null) {
                viewModel.entrarConGoogle(token)
            } else if (error != null) {
                avisar(error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginIniciosesionBtn.setOnClickListener {
            viewModel.entrar(
                binding.loginEmailEdittxt.text.toString(),
                binding.loginPasswordEdittxt.text.toString()
            )
        }

        binding.loginGoogleBtn.setOnClickListener {
            dialogoDeGoogle.launch(AccesoGoogle.cliente(this).signInIntent)
        }

        binding.loginSincuentaTxt.setOnClickListener {
            startActivity(Intent(this, RegistroA::class.java))
        }

        binding.loginOlvidadopaswdTxt.setOnClickListener {
            mostrarRecuperarContrasena()
        }

        observarViewModel()
    }

    private fun observarViewModel() {
        viewModel.sesionIniciada.observe(this) { iniciada ->
            if (iniciada == true) irALaPrincipal()
        }

        viewModel.mensaje.observe(this) { recurso ->
            recurso?.let {
                avisar(getString(it))
                viewModel.mensajeMostrado()
            }
        }

        viewModel.correoDeRecuperacionEnviado.observe(this) { enviado ->
            if (enviado == true) {
                mostrarCorreoEnviado()
                viewModel.recuperacionMostrada()
            }
        }

        viewModel.cargando.observe(this) { cargando ->
            binding.loginProgress.mostrarSi(cargando)
            binding.loginIniciosesionBtn.isEnabled = !cargando
            binding.loginGoogleBtn.isEnabled = !cargando
        }
    }

    /**
     * Antes bastaba con escribir el correo arriba y tocar el enlace, y solo salia un Toast
     * de una linea. Ahora se explica lo que va a pasar y se puede escribir el correo ahi
     * mismo, que es lo logico si has olvidado la contraseña.
     */
    private fun mostrarRecuperarContrasena() {
        // El mismo campo que el resto de dialogos, en vez de un EditText suelto sin margenes.
        val contenido = layoutInflater.inflate(R.layout.dialogo_campo_texto, null)
        val campo = contenido.findViewById<android.widget.EditText>(R.id.dialogoCampo).apply {
            hint = getString(R.string.login_hint_correo)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.loginEmailEdittxt.text.toString())
        }

        DialogoDailyMovie.mostrar(
            context = this,
            titulo = getString(R.string.login_recuperar_titulo),
            mensaje = getString(R.string.login_recuperar_mensaje),
            contenido = contenido,
            textoAceptar = getString(R.string.boton_enviar)
        ) { dialogo ->
            dialogo.dismiss()
            viewModel.recuperarContrasena(campo.text.toString())
        }
    }

    private fun mostrarCorreoEnviado() {
        DialogoDailyMovie.mostrar(
            context = this,
            titulo = getString(R.string.login_correo_enviado_titulo),
            mensaje = getString(R.string.login_correo_enviado_mensaje),
            textoAceptar = getString(R.string.boton_entendido),
            textoCancelar = null
        ) { it.dismiss() }
    }

    private fun irALaPrincipal() {
        startActivity(Intent(this, PrincipalA::class.java))
        finish()
    }

    private fun avisar(texto: String) {
        Avisos.breve(binding.root, texto)
    }

    override fun onStart() {
        super.onStart()
        // Si ya hay sesion abierta no tiene sentido enseñar el formulario.
        if (viewModel.haySesion()) irALaPrincipal()
    }
}
