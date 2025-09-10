package com.example.dailymovie.activities.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dailymovie.activities.viewmodels.LoginViewModel
import com.example.dailymovie.databinding.ActivityLoginBinding
import com.example.dailymovie.utils.AccesoGoogle

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

        viewModel.mensaje.observe(this) { texto ->
            texto?.let {
                avisar(it)
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
            binding.loginProgress.visibility = if (cargando) View.VISIBLE else View.GONE
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
        val campo = android.widget.EditText(this).apply {
            hint = "Tu correo electrónico"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.loginEmailEdittxt.text.toString())
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage(
                "Te mandamos un correo con un enlace para poner una contraseña nueva.\n\n" +
                    "Si no te llega en unos minutos, mira en la carpeta de spam."
            )
            .setView(campo)
            .setPositiveButton("Enviar") { _, _ ->
                viewModel.recuperarContrasena(campo.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarCorreoEnviado() {
        AlertDialog.Builder(this)
            .setTitle("Correo enviado")
            .setMessage(
                "Ya te hemos mandado el enlace. Ábrelo desde el móvil, elige tu contraseña " +
                    "nueva y vuelve aquí a iniciar sesión."
            )
            .setPositiveButton("Entendido", null)
            .show()
    }

    private fun irALaPrincipal() {
        startActivity(Intent(this, PrincipalA::class.java))
        finish()
    }

    private fun avisar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // Si ya hay sesion abierta no tiene sentido enseñar el formulario.
        if (viewModel.haySesion()) irALaPrincipal()
    }
}
