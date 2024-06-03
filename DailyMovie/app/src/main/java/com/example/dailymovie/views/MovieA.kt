package com.example.dailymovie.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.adapters.*
import com.example.dailymovie.client.FirebaseClient
import com.example.dailymovie.client.response.*
import com.example.dailymovie.models.MovieDetailsModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.viewmodels.MovieViewModel
import com.example.dailymovie.databinding.ActivityMovieBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.utils.LocaleUtil
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class MovieA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityMovieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieId = intent.getIntExtra("MOVIE_ID", -1)

        if (movieId != -1) {
            movieViewModel.fetchMovieDetails(movieId, Constantes.API_KEY, LocaleUtil.getLanguageAndCountry())
            movieViewModel.fetchMovieProviders(movieId, Constantes.API_KEY)
            movieViewModel.fetchMovieCredits(movieId, Constantes.API_KEY)
            movieViewModel.fetchMovieVideos(movieId, Constantes.API_KEY)
            movieViewModel.fetchSimilarMovies(movieId, Constantes.API_KEY, LocaleUtil.getLanguageAndCountry())
            movieViewModel.fetchRecommendedMovies(movieId, Constantes.API_KEY, LocaleUtil.getLanguageAndCountry())

            movieViewModel.movieDetails.observe(this, Observer { movieDetailsResponse ->
                movieDetailsResponse?.let {
                    val movieDetails = convertToMovieDetailsModel(it)
                    displayMovieDetails(movieDetails)
                    setupButtons(movieDetails)
                }
            })

            movieViewModel.movieProviders.observe(this, Observer { providerResponse ->
                providerResponse?.let {
                    displayProviders(it)
                }
            })

            movieViewModel.movieCredits.observe(this, Observer { creditResponse ->
                creditResponse?.let {
                    displayCredits(it)
                    binding.txtDirectorMovie.text = getDirectorName(it)
                }
            })

            movieViewModel.movieVideos.observe(this, Observer { videoList ->
                videoList?.let {
                    displayVideos(it)
                }
            })

            movieViewModel.similarMovies.observe(this, Observer { similarMovies ->
                similarMovies?.let {
                    displaySimilarMovies(it)
                }
            })

            movieViewModel.recommendedMovies.observe(this, Observer { recommendedMovies ->
                recommendedMovies?.let {
                    displayRecommendedMovies(it)
                }
            })
        } else {
            showToast("Error: El Id de la pelicula no es valido.")
        }
    }

    private fun setupButtons(movie: MovieDetailsModel) {
        val movieModel = MovieModel(
            id = movie.id,
            title = movie.title,
            releaseDate = movie.releaseDate,
            voteAverage = movie.voteAverage,
            posterPath = movie.posterPath
        )

        val btnFavorite: ImageButton = findViewById(R.id.btnFavorite)
        val btnWatched: ImageButton = findViewById(R.id.btnWatched)
        val btnAddList: ImageButton = findViewById(R.id.btnAddList)
        val btnShare: ImageButton = findViewById(R.id.btnShare)

        updateFavoriteButtonIcon(movie.id, btnFavorite)
        updateWatchedButtonIcon(movie.id, btnWatched)

        btnFavorite.setOnClickListener {
            movieViewModel.toggleFavorite(movieModel) { isFavorite ->
                updateFavoriteButtonIcon(movie.id, btnFavorite)
            }
        }

        btnWatched.setOnClickListener {
            movieViewModel.toggleWatched(movieModel) { isWatched ->
                updateWatchedButtonIcon(movie.id, btnWatched)
            }
        }
        btnAddList.setOnClickListener {
            showListSelectionDialog(movieModel)
        }
        btnShare.setOnClickListener {
            shareMovie(movieModel)
        }
    }

    private fun shareMovie(movie: MovieModel) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Te recomiendo esta pelicula: ${movie.title}\n${Constantes.BASE_MOVIE_URL}${movie.id}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir pelicula via"))
    }

    private fun updateFavoriteButtonIcon(movieId: Int, button: ImageButton) {
        FirebaseClient.isMovieInFavorites(movieId) { isFavorite ->
            runOnUiThread {
                val icon = if (isFavorite) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24
                button.setImageResource(icon)
            }
        }
    }

    private fun updateWatchedButtonIcon(movieId: Int, button: ImageButton) {
        FirebaseClient.isMovieInWatched(movieId) { isWatched ->
            runOnUiThread {
                val icon = if (isWatched) R.drawable.ic_baseline_visibility_24 else R.drawable.ic_baseline_visibility_off_24
                button.setImageResource(icon)
            }
        }
    }

    private fun showListSelectionDialog(movie: MovieModel) {
        movieViewModel.getCustomLists { listNames ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Seleccionar lista")

            builder.setItems(listNames.toTypedArray()) { dialog, which ->
                val selectedList = listNames[which]
                movieViewModel.addMovieToList(selectedList, movie) { success ->
                    if (success) {
                        showToast("Película añadida a $selectedList")
                    } else {
                        showToast("La película ya está en la lista $selectedList")
                    }
                }
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
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
        binding.txtTaglineMovie.text = movie.tagline
        binding.txtTaglineMovie.visibility = if (movie.tagline.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.txtDirectorMovie.text = getDirectorName(movieViewModel.movieCredits.value)

        val releaseYear = movie.releaseDate.take(4)
        binding.txtAnioLabel.visibility = if (releaseYear.isEmpty()) View.GONE else View.VISIBLE
        binding.txtAnioMovie.text = releaseYear
        binding.txtAnioMovie.visibility = if (releaseYear.isEmpty()) View.GONE else View.VISIBLE

        binding.txtValoracionLabel.visibility = if (movie.voteAverage == 0.0) View.GONE else View.VISIBLE
        binding.txtValoracionMovie.text = movie.voteAverage.toString()
        binding.txtValoracionMovie.visibility = if (movie.voteAverage == 0.0) View.GONE else View.VISIBLE

        binding.txtOverviewLabel.visibility = if (movie.overview.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.txtOverviewMovie.text = movie.overview
        binding.txtOverviewMovie.visibility = if (movie.overview.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.txtGenresLabel.visibility = if (movie.genres.isEmpty()) View.GONE else View.VISIBLE
        binding.txtGenresMovie.text = movie.genres.joinToString { it.name }
        binding.txtGenresMovie.visibility = if (movie.genres.isEmpty()) View.GONE else View.VISIBLE

        binding.txtRuntimeLabel.visibility = if (movie.runtime == 0) View.GONE else View.VISIBLE
        binding.txtRuntimeMovie.text = "${movie.runtime} min"
        binding.txtRuntimeMovie.visibility = if (movie.runtime == 0) View.GONE else View.VISIBLE

        binding.txtBudgetLabel.visibility = if (movie.budget == 0) View.GONE else View.VISIBLE
        binding.txtBudgetMovie.text = "${movie.budget} USD"
        binding.txtBudgetMovie.visibility = if (movie.budget == 0) View.GONE else View.VISIBLE

        binding.txtRevenueLabel.visibility = if (movie.revenue == 0L) View.GONE else View.VISIBLE
        binding.txtRevenueMovie.text = "${movie.revenue} USD"
        binding.txtRevenueMovie.visibility = if (movie.revenue == 0L) View.GONE else View.VISIBLE

        Glide.with(this)
            .load(Constantes.IMAGE_URL + movie.posterPath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(binding.imgPosterMovie)

        val hasContent = binding.txtDirectorMovie.text.isNotEmpty() ||
                binding.txtRuntimeMovie.text.isNotEmpty() ||
                binding.txtAnioMovie.text.isNotEmpty() ||
                binding.txtValoracionMovie.text.isNotEmpty() ||
                binding.txtOverviewMovie.text.isNotEmpty() ||
                binding.txtGenresMovie.text.isNotEmpty() ||
                binding.txtBudgetMovie.text.isNotEmpty() ||
                binding.txtRevenueMovie.text.isNotEmpty()

        binding.sectionDetails.visibility = if (hasContent) View.VISIBLE else View.GONE
    }

    private fun getDirectorName(credits: CreditResponse?): String {
        credits?.let {
            it.crew.forEach { crewMember ->
                if (crewMember.job == "Director") {
                    return crewMember.name
                }
            }
            it.cast.forEach { castMember ->
                if (castMember.character == "Director") {
                    return castMember.name
                }
            }
        }
        return ""
    }

    private fun displayProviders(providerResponse: ProviderResponse) {
        val providers = providerResponse.results["ES"]?.flatrate ?: emptyList()
        if (providers.isNotEmpty()) {
            val adapter = ProviderAdapter(providers)
            binding.recyclerViewProviders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewProviders.adapter = adapter
            binding.sectionProviders.visibility = View.VISIBLE
            binding.recyclerViewProviders.addItemDecoration(SpacingItemDecoration(spacing = 8))
        } else {
            binding.sectionProviders.visibility = View.GONE
        }
    }

    private fun displayCredits(creditResponse: CreditResponse) {
        val cast = creditResponse.cast
        if (cast.isNotEmpty()) {
            val adapter = CreditAdapter(cast)
            binding.recyclerViewCredits.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewCredits.adapter = adapter
            binding.sectionCredits.visibility = View.VISIBLE
            binding.recyclerViewCredits.addItemDecoration(SpacingItemDecoration(spacing = 8))
        } else {
            binding.sectionCredits.visibility = View.GONE
        }
    }

    private fun displayVideos(videoList: List<VideoModel>) {
        if (videoList.isNotEmpty()) {
            val adapter = VideoAdapter(this, videoList)
            binding.viewPagerVideos.adapter = adapter
            binding.sectionVideos.visibility = View.VISIBLE
        } else {
            binding.sectionVideos.visibility = View.GONE
        }
    }

    private fun displaySimilarMovies(similarMovies: List<MovieModel>) {
        if (similarMovies.isNotEmpty()) {
            val adapter = NowPlayingAdapter(ArrayList(similarMovies))
            binding.recyclerViewSimilarMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewSimilarMovies.adapter = adapter
            binding.sectionSimilarMovies.visibility = View.VISIBLE
            binding.recyclerViewSimilarMovies.addItemDecoration(SpacingItemDecoration(spacing = 8))
        } else {
            binding.sectionSimilarMovies.visibility = View.GONE
        }
    }

    private fun displayRecommendedMovies(recommendedMovies: List<MovieModel>) {
        if (recommendedMovies.isNotEmpty()) {
            val adapter = NowPlayingAdapter(ArrayList(recommendedMovies))
            binding.recyclerViewRecommendedMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewRecommendedMovies.adapter = adapter
            binding.sectionRecommendedMovies.visibility = View.VISIBLE
            binding.recyclerViewRecommendedMovies.addItemDecoration(SpacingItemDecoration(spacing = 8))
        } else {
            binding.sectionRecommendedMovies.visibility = View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewPagerVideos.children.forEach {
            if (it is YouTubePlayerView) {
                it.release()
            }
        }
    }
}
