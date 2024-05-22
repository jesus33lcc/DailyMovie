package com.example.dailymovie.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.adapters.ListaImborrableAdapter
import com.example.dailymovie.databinding.FragmentListasBinding
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.views.ListMoviesA

class ListasF : Fragment() {

    private var _binding: FragmentListasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListasViewModel by viewModels()

    private lateinit var listaImborrableAdapter: ListaImborrableAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.misListasCheckFav.layoutManager = LinearLayoutManager(context)

        viewModel.favoriteAndWatchedLists.observe(viewLifecycleOwner, Observer { listas ->
            initializeListFillInmborrable(listas)
        })
    }

    private fun initializeListFillInmborrable(listaListasFill: List<ListaModel>) {
        listaImborrableAdapter = ListaImborrableAdapter(requireActivity(), listaListasFill) { lista ->
            when (lista.nombre) {
                "Favoritos" -> {
                    viewModel.getFavorites { movieList ->
                        navigateToMovieList(movieList, lista.nombre)
                    }
                }
                "Vistos" -> {
                    viewModel.getWatched { movieList ->
                        navigateToMovieList(movieList, lista.nombre)
                    }
                }
            }
        }
        binding.misListasCheckFav.adapter = listaImborrableAdapter
    }

    private fun navigateToMovieList(movieList: List<MovieModel>, listName: String) {
        val intent = Intent(context, ListMoviesA::class.java)
        intent.putParcelableArrayListExtra("MOVIE_LIST", ArrayList(movieList))
        intent.putExtra("LIST_NAME", listName)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}