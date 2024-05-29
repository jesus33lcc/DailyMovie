package com.example.dailymovie.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.adapters.NowPlayingAdapter
import com.example.dailymovie.databinding.FragmentHomeBinding
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.Constantes

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
        binding.recyclerViewPopular.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewTopRated.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewUpcoming.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        homeViewModel.nowPlayingMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewNowPlaying.adapter = NowPlayingAdapter(movies)
        })
        homeViewModel.popularMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewPopular.adapter = NowPlayingAdapter(movies)
        })
        homeViewModel.topRatedMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewTopRated.adapter = NowPlayingAdapter(movies)
        })
        homeViewModel.upcomingMovies.observe(viewLifecycleOwner, Observer { movies ->
            binding.recyclerViewUpcoming.adapter = NowPlayingAdapter(movies)
        })

        homeViewModel.movieOfTheDay.observe(viewLifecycleOwner, Observer { movie ->
            movie?.let {
                Log.d("HomeF", "Movie of the day: ${it.title}")
                displayMovieOfTheDay(it)
            } ?: Log.d("HomeF", "Movie of the day is null")
        })

        homeViewModel.fetchNowPlayingMovies(Constantes.API_KEY)
        homeViewModel.fetchPopularMovies(Constantes.API_KEY)
        homeViewModel.fetchTopRatedMovies(Constantes.API_KEY)
        homeViewModel.fetchUpcomingMovies(Constantes.API_KEY)
        homeViewModel.fetchMovieOfTheDay()
    }

    private fun displayMovieOfTheDay(movie: MovieOfTheDay) {
        binding.movieTitle.text = movie.title
        binding.movieRating.text = "Rating: ${movie.rating}"
        binding.movieReview.text = movie.review
        binding.movieDate.text = "Fecha: ${movie.date}"
        binding.movieAuthor.text = "Autor: ${movie.author}"

        Glide.with(this)
            .load(Constantes.IMAGE_URL + movie.posterPath)
            .into(binding.moviePoster)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}