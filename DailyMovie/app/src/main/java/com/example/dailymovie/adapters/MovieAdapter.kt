package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.databinding.ItemMovieCardBinding
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.activities.views.MovieA

class MovieAdapter(private val movies: List<MovieModel>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    class MovieViewHolder(private val binding: ItemMovieCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieModel) {
            binding.movieTitle.text = movie.title
            Glide.with(binding.root.context)
                .load(Constantes.IMAGE_URL + movie.posterPath)
                .into(binding.moviePoster)

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, MovieA::class.java).apply {
                    putExtra("MOVIE_ID", movie.id)
                }
                context.startActivity(intent)
            }
        }
    }
}