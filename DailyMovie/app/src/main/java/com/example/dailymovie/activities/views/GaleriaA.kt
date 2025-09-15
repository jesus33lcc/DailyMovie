package com.example.dailymovie.activities.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.databinding.ActivityGaleriaBinding
import com.example.dailymovie.utils.Constantes

/**
 * Las imagenes de una pelicula a pantalla completa.
 *
 * TMDB tiene muchisimas (de El Padrino hay 135 fondos) y antes no se usaba ninguna, solo
 * el cartel. Aqui se pasan deslizando y se cargan en grande, no en el tamaño de miniatura.
 */
class GaleriaA : AppCompatActivity() {

    private lateinit var binding: ActivityGaleriaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGaleriaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rutas = intent.getStringArrayListExtra(EXTRA_IMAGENES).orEmpty()
        val empezarEn = intent.getIntExtra(EXTRA_POSICION, 0)

        if (rutas.isEmpty()) {
            finish()
            return
        }

        binding.paginadorImagenes.adapter = Adaptador(rutas)
        binding.paginadorImagenes.setCurrentItem(empezarEn, false)

        actualizarContador(empezarEn, rutas.size)
        binding.paginadorImagenes.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = actualizarContador(position, rutas.size)
            }
        )
    }

    private fun actualizarContador(posicion: Int, total: Int) {
        binding.contadorImagenes.text = "${posicion + 1} / $total"
    }

    private class Adaptador(private val rutas: List<String>) :
        RecyclerView.Adapter<Adaptador.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_imagen_completa, parent, false) as ImageView
        )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            Glide.with(holder.imagen.context)
                // A pantalla completa hace falta el tamaño grande, no el w500 de las listas.
                .load(Constantes.IMAGE_ORIGINAL_URL + rutas[position])
                .placeholder(R.drawable.ic_baseline_image_24)
                .error(R.drawable.ic_baseline_image_24)
                .into(holder.imagen)
        }

        override fun getItemCount() = rutas.size

        class Holder(val imagen: ImageView) : RecyclerView.ViewHolder(imagen)
    }

    companion object {
        const val EXTRA_IMAGENES = "IMAGENES"
        const val EXTRA_POSICION = "POSICION"
    }
}
