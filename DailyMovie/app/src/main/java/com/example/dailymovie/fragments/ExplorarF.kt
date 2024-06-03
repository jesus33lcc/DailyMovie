package com.example.dailymovie.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.SearchMovieAdapter
import com.example.dailymovie.databinding.FragmentExplorarBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.viewmodels.ExplorarViewModel
import com.example.dailymovie.views.MovieA

class ExplorarF : Fragment() {

    private val explorarViewModel: ExplorarViewModel by viewModels()
    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!
    private lateinit var movieAdapter: SearchMovieAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentExplorarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        movieAdapter = SearchMovieAdapter(arrayListOf()) { movie ->
            explorarViewModel.addToHistory(movie)
            val intent = Intent(activity, MovieA::class.java).apply {
                putExtra("MOVIE_ID", movie.id)
            }
            startActivity(intent)
        }
        binding.rvListaBusqueda.layoutManager = LinearLayoutManager(activity)
        binding.rvListaBusqueda.adapter = movieAdapter
        binding.rvListaBusqueda.addItemDecoration(SpacingItemDecoration(spacing = 8))

        // Setup Search EditText
        val searchInput: EditText = binding.searchInput
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && s.length > 2) {
                    buscar(s.toString())
                } else if (s.isNullOrEmpty()) {
                    clearResults()
                    loadHistory()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        explorarViewModel.movies.observe(viewLifecycleOwner, Observer { movies ->
            updateUIWithResults(movies)
        })

        explorarViewModel.history.observe(viewLifecycleOwner, Observer { history ->
            if (history.isNotEmpty()) {
                movieAdapter.updateMoviesList(history.reversed())
            }
        })

        loadHistory()
    }

    private fun buscar(query: String) {
        explorarViewModel.searchMovies(query)
    }

    private fun loadHistory() {
        explorarViewModel.loadHistory()
    }

    private fun updateUIWithResults(results: List<MovieModel>) {
        Handler(Looper.getMainLooper()).post {
            movieAdapter.updateMoviesList(ArrayList(results))
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearResults() {
        movieAdapter.updateMoviesList(emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
