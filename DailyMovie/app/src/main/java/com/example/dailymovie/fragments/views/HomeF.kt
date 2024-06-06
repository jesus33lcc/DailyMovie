package com.example.dailymovie.fragments.views

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.adapters.MovieAdapter
import com.example.dailymovie.databinding.FragmentHomeBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.fragments.viewmodels.HomeViewModel
import com.example.dailymovie.activities.views.MovieA
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeF : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var youTubePlayerView: YouTubePlayerView

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

        homeViewModel.fetchNowPlayingMovies(Constantes.API_KEY)
        homeViewModel.fetchPopularMovies(Constantes.API_KEY)
        homeViewModel.fetchTopRatedMovies(Constantes.API_KEY)
        homeViewModel.fetchUpcomingMovies(Constantes.API_KEY)
        homeViewModel.fetchMovieOfTheDay()

        youTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView)
    }

    private fun displayMovieOfTheDay(movie: MovieOfTheDay) {
        binding.movieTitle.text = movie.title
        binding.movieReview.text = movie.review

        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.movieDate.text = currentDate

        binding.movieAuthor.text = movie.author

        binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(movie.videoId, 0f)
            }
        })

        binding.btnViewFullDetails.setOnClickListener {
            val intent = Intent(context, MovieA::class.java)
            intent.putExtra("MOVIE_ID", movie.id)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
