package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes

/**
 * Las peliculas que hay dentro de una lista.
 *
 * Es un ListAdapter: se le pasa la lista entera con submitList y el comparador averigua solo
 * lo que ha cambiado. Antes se repintaba todo con notifyDataSetChanged, que ademas de hacer
 * trabajo de mas se carga las animaciones de entrada y salida de la fila.
 */
class CustomMovieListAdapter(
    private val onMovieClick: (MovieModel) -> Unit,
    private val onMovieDelete: (MovieModel, Int) -> Unit
) : ListAdapter<MovieModel, CustomMovieListAdapter.MovieViewHolder>(COMPARADOR) {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txt_titleMovie)
        val year: TextView = itemView.findViewById(R.id.txt_yearMovie)
        val poster: ImageView = itemView.findViewById(R.id.img_posterMovie)
        val rating: TextView = itemView.findViewById(R.id.txt_voteAverageMovie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_list, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.title.text = movie.title
        holder.year.text = movie.releaseDate.take(4)
        holder.rating.text = "Rating: ${movie.voteAverage}"
        Glide.with(holder.itemView.context)
            .load(Constantes.IMAGE_URL + movie.posterPath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.poster)

        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }

        holder.itemView.setOnLongClickListener {
            onMovieDelete(movie, holder.bindingAdapterPosition)
            true
        }
    }

    /** La que esta en esa fila, para cuando el usuario desliza para borrar. */
    fun peliculaEn(position: Int): MovieModel = getItem(position)

    private companion object {
        val COMPARADOR = object : DiffUtil.ItemCallback<MovieModel>() {
            // Es la misma pelicula si comparten id, aunque le haya cambiado la nota.
            override fun areItemsTheSame(vieja: MovieModel, nueva: MovieModel) =
                vieja.id == nueva.id

            override fun areContentsTheSame(vieja: MovieModel, nueva: MovieModel) =
                vieja == nueva
        }
    }
}
