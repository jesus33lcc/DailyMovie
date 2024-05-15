package com.example.dailymovie.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.databinding.ActivityMovieBinding
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.viewmodels.MovieViewModel

class MovieA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityMovieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val movie = intent.getParcelableExtra<MovieModel>("MOVIE")

        movie?.let {
            movieViewModel.setMovie(it)
            displayMovieDetails(it)
            setupButtons(it)
        }
    }

    private fun displayMovieDetails(movie: MovieModel) {
        binding.txtTituloMovie.text = movie.title
        binding.txtDirectorMovie.text = movie.originalTitle // Cambiado a originalTitle
        binding.txtAnioMovie.text = movie.releaseDate.take(4)
        binding.txtValoracionMovie.text = movie.voteAverage.toString()
        binding.textDescripcion.text = movie.overview
        Glide.with(this)
            .load(Constantes.IMAGE_URL + movie.posterPath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(binding.imgPosterMovie)
    }
    private fun setupButtons(movie: MovieModel) {
        val movieId = movie.id

        binding.tBtnLikeMovie.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                movieViewModel.addToFavorites(movieId) { success ->
                    if (success) {
                        showToast("Añadido a Favoritos")
                    } else {
                        showToast("Ya existe en Favoritos")
                    }
                }
            } else {
                movieViewModel.removeFromFavorites(movieId) { success ->
                    if (success) {
                        showToast("Eliminado de Favoritos")
                    } else {
                        showToast("Error al eliminar de Favoritos")
                    }
                }
            }
        }

        binding.tBtnViewMovie.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                movieViewModel.addToWatched(movieId) { success ->
                    if (success) {
                        showToast("Añadido a Vistos")
                    } else {
                        showToast("Ya existe en Vistos")
                    }
                }
            } else {
                movieViewModel.removeFromWatched(movieId) { success ->
                    if (success) {
                        showToast("Eliminado de Vistos")
                    } else {
                        showToast("Error al eliminar de Vistos")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}