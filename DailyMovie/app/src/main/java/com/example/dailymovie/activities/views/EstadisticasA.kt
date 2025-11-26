package com.example.dailymovie.activities.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dailymovie.R
import com.example.dailymovie.activities.viewmodels.EstadisticasViewModel
import com.example.dailymovie.activities.viewmodels.Numero
import com.example.dailymovie.databinding.ActivityEstadisticasBinding

/**
 * Tus números: cuánto has visto, de qué y de cuándo.
 *
 * Es una pantalla de solo lectura y de pocos datos, así que las fichas se inflan a mano en un
 * LinearLayout en vez de montar un RecyclerView con su adaptador: no hay nada que reciclar y
 * el código queda bastante más corto.
 */
class EstadisticasA : AppCompatActivity() {

    private lateinit var binding: ActivityEstadisticasBinding
    private val viewModel: EstadisticasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEstadisticasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverNumeros.setOnClickListener { finish() }

        viewModel.numeros.observe(this) { numeros ->
            binding.progresoNumeros.visibility = View.GONE
            pintar(numeros)
        }
        viewModel.cargar()
    }

    private fun pintar(numeros: List<Numero>) {
        binding.contenedorNumeros.removeAllViews()

        // Sin nada guardado los ceros no dicen nada, así que se explica qué hacer para que
        // esta pantalla tenga algo que contar.
        if (numeros.all { it.numero == "0" }) {
            binding.contenedorNumeros.addView(
                TextView(this).apply {
                    text = "Todavía no hay nada que contar.\n\n" +
                        "Marca películas como vistas o guárdalas en favoritos y aquí verás " +
                        "en qué se te va el tiempo."
                    textSize = 17f
                    setPadding(24, 48, 24, 24)
                    gravity = android.view.Gravity.CENTER
                    setTextColor(getColor(R.color.white))
                }
            )
            return
        }

        numeros.forEach { numero ->
            val ficha = LayoutInflater.from(this)
                .inflate(R.layout.item_numero, binding.contenedorNumeros, false)
            ficha.findViewById<TextView>(R.id.txtNumero).text = numero.numero
            ficha.findViewById<TextView>(R.id.txtQueEs).text = numero.queEs
            binding.contenedorNumeros.addView(ficha)
        }
    }
}
