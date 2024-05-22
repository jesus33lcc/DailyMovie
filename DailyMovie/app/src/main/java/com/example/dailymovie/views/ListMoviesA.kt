package com.example.dailymovie.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.MovieAdapter
import com.example.dailymovie.databinding.ActivityListMoviesBinding
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.viewmodels.ListMoviesViewModel

class ListMoviesA : AppCompatActivity() {

    private lateinit var binding: ActivityListMoviesBinding
    private val viewModel: ListMoviesViewModel by viewModels()
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieList: List<MovieModel> = intent.getParcelableArrayListExtra("MOVIE_LIST") ?: emptyList()
        val listName: String? = intent.getStringExtra("LIST_NAME")

        viewModel.setMovies(movieList)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = listName

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(arrayListOf())
        binding.recyclerViewMovies.apply {
            layoutManager = LinearLayoutManager(this@ListMoviesA)
            adapter = movieAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.movies.observe(this, { movies ->
            movieAdapter.updateMoviesList(ArrayList(movies))
        })
    }
}