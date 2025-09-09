package com.example.dailymovie.fragments.views

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.adapters.MovieAdapter
import com.example.dailymovie.databinding.FragmentHomeBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.Trailers
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.fragments.viewmodels.HomeViewModel
import com.example.dailymovie.activities.views.MovieA
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeF : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        binding.recyclerViewNowPlaying.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewNowPlaying.addItemDecoration(SpacingItemDecoration(spacing = 8))
        binding.recyclerViewPopular.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewPopular.addItemDecoration(SpacingItemDecoration(spacing = 8))
        binding.recyclerViewTopRated.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewTopRated.addItemDecoration(SpacingItemDecoration(spacing = 8))
        binding.recyclerViewUpcoming.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewUpcoming.addItemDecoration(SpacingItemDecoration(spacing = 8))

        homeViewModel.nowPlayingMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewNowPlaying.adapter = MovieAdapter(movies)
        })
        homeViewModel.popularMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewPopular.adapter = MovieAdapter(movies)
        })
        homeViewModel.topRatedMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewTopRated.adapter = MovieAdapter(movies)
        })
        homeViewModel.upcomingMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewUpcoming.adapter = MovieAdapter(movies)
        })

        homeViewModel.movieOfTheDay.observe(viewLifecycleOwner, Observer { movie ->
            movie?.let {
                Log.d("HomeF", "La pelicula del dia es: ${it.title}")
                displayMovieOfTheDay(it)
            } ?: Log.d("HomeF", "No hay pelicula del dia")
        })

        homeViewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Toast.makeText(context, getString(it.mensaje()), Toast.LENGTH_SHORT).show()
                homeViewModel.errorMostrado()
            }
        })

        homeViewModel.cargando.observe(viewLifecycleOwner, Observer { cargando ->
            binding.swipeRefreshLayout.isRefreshing = cargando
        })

        homeViewModel.cargarPortada()



        setupSwipeRefreshLayout()
    }

    private fun setupSwipeRefreshLayout() {
        // El circulo se para solo cuando el ViewModel avisa de que ha contestado todo.
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.cargarPortada()
        }
    }

    private fun displayMovieOfTheDay(movie: MovieOfTheDay) {
        binding.movieTitle.text = movie.title
        binding.movieReview.text = movie.review

        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.movieDate.text = hoy

        // En la pelicula curada a mano el autor es quien escribio la reseña; en una
        // recomendada, el motivo por el que se le enseña ("Porque te gusta...").
        binding.movieAuthor.text = movie.author

        mostrarTrailer(movie.videoId)

        binding.btnViewFullDetails.setOnClickListener {
            val intent = Intent(context, MovieA::class.java)
            intent.putExtra("MOVIE_ID", movie.id)
            startActivity(intent)
        }
    }

    /**
     * Enseña la miniatura del trailer, o quita el hueco si esa pelicula no tiene ninguno.
     *
     * YouTube ya no deja reproducir dentro de un WebView de Android, asi que al tocar la
     * miniatura se abre YouTube. Ver utils/Trailers.
     */
    private fun mostrarTrailer(videoId: String) {
        if (videoId.isBlank()) {
            binding.trailerContainer.visibility = View.GONE
            return
        }

        binding.trailerContainer.visibility = View.VISIBLE
        Glide.with(this)
            .load(Trailers.miniatura(videoId))
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(binding.trailerThumbnail)

        binding.trailerContainer.setOnClickListener {
            Trailers.abrir(requireContext(), videoId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
