package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.activities.views.MovieA
import com.example.dailymovie.client.response.PeliculaDePersona
import com.example.dailymovie.databinding.ItemMovieCardBinding
import com.example.dailymovie.utils.Constantes

/**
 * Las peliculas de la filmografia de una persona.
 *
 * Reaprovecha item_movie_card, la misma ficha que usan las listas de la portada, para que
 * todo el catalogo se vea igual en cualquier pantalla.
 */
class PeliculaDePersonaAdapter(
    private val peliculas: List<PeliculaDePersona>
) : RecyclerView.Adapter<PeliculaDePersonaAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.pintar(peliculas[position])

    override fun getItemCount() = peliculas.size

    class Holder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun pintar(pelicula: PeliculaDePersona) {
            binding.movieTitle.text = pelicula.titulo

            binding.moviePoster.cargarCartel(pelicula.poster)

            binding.root.setOnClickListener {
                val contexto = binding.root.context
                contexto.startActivity(
                    Intent(contexto, MovieA::class.java).putExtra("MOVIE_ID", pelicula.id)
                )
            }
        }
    }
}
