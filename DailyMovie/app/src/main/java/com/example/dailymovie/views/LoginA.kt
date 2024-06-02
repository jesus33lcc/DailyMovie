package com.example.dailymovie.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.dailymovie.databinding.ActivityLoginBinding
import com.example.dailymovie.viewmodels.LoginViewModel

class LoginA : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginIniciosesionBtn.setOnClickListener {
            val email = binding.loginEmailEdittxt.text.toString().trim()
            val password = binding.loginPasswordEdittxt.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.loginUser(email, password)
            } else {
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginSincuentaTxt.setOnClickListener {
            val intent = Intent(this, RegistroA::class.java)
            startActivity(intent)
        }

        binding.loginOlvidadopaswdTxt.setOnClickListener {
            val email = binding.loginEmailEdittxt.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.resetPassword(email)
            } else {
                Toast.makeText(this, "Por favor, introduce tu correo electrónico para restablecer la contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginStatus.observe(this) { status ->
            status?.let {
                if (it) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Error en el inicio de sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.resetPasswordStatus.observe(this) { status ->
            status?.let {
                if (it) {
                    Toast.makeText(this, "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al enviar el correo de recuperación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, PrincipalA::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = viewModel.getCurrentUser()
        if (currentUser != null) {
            navigateToMain()
        }
    }
}
