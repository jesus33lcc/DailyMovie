package com.example.dailymovie.fragments

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.SearchMovieAdapter
import com.example.dailymovie.databinding.FragmentExplorarBinding
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.viewmodels.ExplorarViewModel
import com.example.dailymovie.views.MovieA

class ExplorarF : Fragment() {

    private val explorarViewModel: ExplorarViewModel by viewModels()
    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!
    private lateinit var movieAdapter: SearchMovieAdapter
    private lateinit var toolbarSearch: Toolbar
    private lateinit var searchViewExplorar: SearchView

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

        // Setup Toolbar and SearchView
        toolbarSearch = binding.myToolbarExplorer
        toolbarSearch.inflateMenu(R.menu.toolbar_search_explorar)
        val searchItem: MenuItem = toolbarSearch.menu.findItem(R.id.action_searchExplore)
        searchViewExplorar = searchItem.actionView as SearchView
        searchViewExplorar.queryHint = "Buscar titulo..."

        val colorNegro = ContextCompat.getColor(requireContext(), android.R.color.black)
        val searchEditText: EditText = searchViewExplorar.findViewById(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(colorNegro)
        searchEditText.setHintTextColor(colorNegro)
        val iconoSearch: Drawable? = searchItem.icon
        iconoSearch?.setColorFilter(colorNegro, PorterDuff.Mode.SRC_ATOP)

        searchViewExplorar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchViewExplorar.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty() && newText.length > 2) {
                    buscar(newText)
                } else if (newText.isNullOrEmpty()) {
                    clearResults()
                    loadHistory()
                }
                return true
            }
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
