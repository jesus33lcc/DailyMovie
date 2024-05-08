package com.example.dailymovie.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.example.dailymovie.R
import com.example.dailymovie.databinding.ActivityRegistroBinding
import com.example.dailymovie.viewmodels.RegistroViewModel

class RegistroA : AppCompatActivity() {
    private lateinit var binding:ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registroRegistrarseBtn.setOnClickListener {
            val email = binding.registroEmailEdittxt.text.toString().trim()
            val password = binding.registroPasswordEdittxt.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.registerUser(email, password)
            } else {
                Toast.makeText(this, "Email y contraseña no pueden estar vacíos", Toast.LENGTH_SHORT).show()
            }
        }
        binding.registroRegistrogoogleBtn.setOnClickListener {
            Toast.makeText(this, "Aun no esta hecho", Toast.LENGTH_SHORT).show()
        }
        viewModel.registrationStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginA::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
            }
        }
        binding.registroCuentacreadoTxt.setOnClickListener {
            val intent = Intent(this, LoginA::class.java)
            startActivity(intent)
            finish()
        }
    }
}