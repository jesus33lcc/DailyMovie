package com.example.dailymovie.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.dailymovie.R
import com.example.dailymovie.databinding.ActivityLoginBinding
import com.example.dailymovie.viewmodels.LoginViewModel

class LoginA : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }
    private fun setupListeners() {
        binding.loginIniciosesionBtn.setOnClickListener {
            val email = binding.loginEmailEdittxt.text.toString().trim()
            val password = binding.loginPasswordEdittxt.text.toString().trim()
            viewModel.loginUser(email, password)
        }

        binding.loginIniciogoogleBtn.setOnClickListener {
            Toast.makeText(this, "XD", Toast.LENGTH_SHORT).show()
        }

        viewModel.loginStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Fallo en el inicio de sesión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}