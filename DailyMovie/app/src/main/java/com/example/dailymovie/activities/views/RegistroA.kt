package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dailymovie.databinding.ActivityRegistroBinding
import com.example.dailymovie.activities.viewmodels.RegistroViewModel

class RegistroA : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerBtn.setOnClickListener {
            val email = binding.registerEmailEdittxt.text.toString().trim()
            val password = binding.registerPasswordEdittxt.text.toString().trim()
            val confirmPassword = binding.registerConfirmPasswordEdittxt.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword) {
                viewModel.registerUser(email, password)
            } else {
                Toast.makeText(this, "Verifique que el email y la contraseña no estén vacíos y que las contraseñas coincidan", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.registrationStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginA::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error en el registro", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerLoginTxt.setOnClickListener {
            val intent = Intent(this, LoginA::class.java)
            startActivity(intent)
            finish()
        }
    }
}
