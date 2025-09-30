package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.activities.views.SerieA
import com.example.dailymovie.databinding.ItemMovieCardBinding
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.utils.Constantes

/**
 * Las series en las listas.
 *
 * Reaprovecha item_movie_card, la misma ficha que las peliculas, para que el catalogo se
 * vea igual en toda la app aunque por dentro sean tipos distintos.
 */
class SerieAdapter(private val series: List<SerieModel>) :
    RecyclerView.Adapter<SerieAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.pintar(series[position])

    override fun getItemCount() = series.size

    class Holder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun pintar(serie: SerieModel) {
            binding.movieTitle.text = serie.titulo

            binding.moviePoster.cargarCartel(serie.poster)

            binding.root.setOnClickListener {
                val contexto = binding.root.context
                contexto.startActivity(
                    Intent(contexto, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, serie.id)
                )
            }
        }
    }
}
