package com.example.dailymovie.fragments.views

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.adapters.SearchMovieAdapter
import com.example.dailymovie.databinding.FragmentExplorarBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.fragments.viewmodels.ExplorarViewModel
import com.example.dailymovie.activities.views.MovieA

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

        val searchInput: EditText = binding.searchInput
        val clearSearchIcon: ImageView = binding.clearSearchIcon

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearSearchIcon.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
                if (!s.isNullOrEmpty()) {
                    explorarViewModel.setSearchMode(true)
                    buscar(s.toString())
                } else {
                    clearResults()
                    explorarViewModel.setSearchMode(false)
                    loadHistory()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = searchInput.text.toString()
                if (query.isNotEmpty()) {
                    explorarViewModel.setSearchMode(true)
                    buscar(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }

        clearSearchIcon.setOnClickListener {
            searchInput.text.clear()
            clearResults()
            explorarViewModel.setSearchMode(false)
            loadHistory()
            hideKeyboard()
        }

        explorarViewModel.movies.observe(viewLifecycleOwner, Observer { movies ->
            updateUIWithResults(movies)
        })

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        if (binding.searchInput.text.isNullOrEmpty()) {
            explorarViewModel.setSearchMode(false)
            loadHistory()
        }
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

    private fun clearResults() {
        movieAdapter.updateMoviesList(emptyList())
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
