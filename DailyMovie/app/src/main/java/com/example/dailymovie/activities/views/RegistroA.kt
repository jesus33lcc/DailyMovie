package com.example.dailymovie.activities.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dailymovie.activities.viewmodels.RegistroViewModel
import com.example.dailymovie.databinding.ActivityRegistroBinding
import com.example.dailymovie.utils.AccesoGoogle

class RegistroA : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()

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
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerBtn.setOnClickListener {
            viewModel.registrar(
                binding.registerEmailEdittxt.text.toString(),
                binding.registerPasswordEdittxt.text.toString(),
                binding.registerConfirmPasswordEdittxt.text.toString()
            )
        }

        binding.registerGoogleBtn.setOnClickListener {
            dialogoDeGoogle.launch(AccesoGoogle.cliente(this).signInIntent)
        }

        binding.registerLoginTxt.setOnClickListener {
            startActivity(Intent(this, LoginA::class.java))
            finish()
        }

        viewModel.registrado.observe(this) { registrado ->
            // Recien creada la cuenta se pregunta por los gustos: es lo que despues alimenta
            // la pelicula recomendada de la portada.
            if (registrado == true) {
                startActivity(Intent(this, OnboardingA::class.java))
                finish()
            }
        }

        viewModel.mensaje.observe(this) { texto ->
            texto?.let {
                avisar(it)
                viewModel.mensajeMostrado()
            }
        }

        viewModel.cargando.observe(this) { cargando ->
            binding.registerProgress.visibility = if (cargando) View.VISIBLE else View.GONE
            binding.registerBtn.isEnabled = !cargando
            binding.registerGoogleBtn.isEnabled = !cargando
        }
    }

    private fun avisar(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
    }
}
