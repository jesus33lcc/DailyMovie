package com.example.dailymovie.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.databinding.ActivityMovieBinding
import com.example.dailymovie.models.MovieDetailsModel
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

        val movieModel = intent.getParcelableExtra<MovieModel>("MOVIE")
        val movieId = movieModel?.id ?: -1

        if (movieId != -1) {
            movieViewModel.setCurrentMovieModel(movieModel!!)
            movieViewModel.fetchMovieDetails(movieId, Constantes.API_KEY, "es")
            movieViewModel.movieDetails.observe(this, Observer { movieDetailsResponse ->
                movieDetailsResponse?.let {
                    val movieDetails = convertToMovieDetailsModel(it)
                    displayMovieDetails(movieDetails)
                    setupButtons()
                }
            })
        } else {
            showToast("Error: Movie ID is not valid.")
        }
    }

    private fun convertToMovieDetailsModel(response: MovieDetailsResponse): MovieDetailsModel {
        return MovieDetailsModel(
            adult = response.adult,
            backdropPath = response.backdropPath,
            belongsToCollection = response.belongsToCollection,
            budget = response.budget,
            genres = response.genres,
            homepage = response.homepage,
            id = response.id,
            imdbId = response.imdbId,
            originCountry = response.originCountry,
            originalLanguage = response.originalLanguage,
            originalTitle = response.originalTitle,
            overview = response.overview,
            popularity = response.popularity,
            posterPath = response.posterPath,
            productionCompanies = response.productionCompanies,
            releaseDate = response.releaseDate,
            revenue = response.revenue,
            runtime = response.runtime,
            status = response.status,
            tagline = response.tagline,
            title = response.title,
            video = response.video,
            voteAverage = response.voteAverage,
            voteCount = response.voteCount
        )
    }

    private fun displayMovieDetails(movie: MovieDetailsModel) {
        binding.txtTituloMovie.text = movie.title
        binding.txtDirectorMovie.text = movie.originalTitle
        binding.txtAnioMovie.text = movie.releaseDate.take(4)
        binding.txtValoracionMovie.text = movie.voteAverage.toString()
        binding.textDescripcion.text = movie.overview
        Glide.with(this)
            .load(Constantes.IMAGE_URL + movie.posterPath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(binding.imgPosterMovie)
    }

    private fun setupButtons() {
        binding.tBtnLikeMovie.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                movieViewModel.addToFavorites { success ->
                    if (success) {
                        showToast("Añadido a Favoritos")
                    } else {
                        showToast("Ya existe en Favoritos")
                    }
                }
            } else {
                movieViewModel.removeFromFavorites { success ->
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
                movieViewModel.addToWatched { success ->
                    if (success) {
                        showToast("Añadido a Vistos")
                    } else {
                        showToast("Ya existe en Vistos")
                    }
                }
            } else {
                movieViewModel.removeFromWatched { success ->
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
