package com.example.dailymovie.fragments.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.adapters.SerieAdapter
import com.example.dailymovie.databinding.FragmentSeriesBinding
import com.example.dailymovie.fragments.viewmodels.SeriesViewModel
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.utils.mensaje

/** La pestaña de series, con la misma pinta que la portada de peliculas. */
class SeriesF : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SeriesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prepararLista(binding.recyclerEnEmision)
        prepararLista(binding.recyclerSeriesPopulares)
        prepararLista(binding.recyclerSeriesMejorValoradas)

        viewModel.enEmision.observe(viewLifecycleOwner) {
            binding.recyclerEnEmision.adapter = SerieAdapter(it)
        }
        viewModel.populares.observe(viewLifecycleOwner) {
            binding.recyclerSeriesPopulares.adapter = SerieAdapter(it)
        }
        viewModel.mejorValoradas.observe(viewLifecycleOwner) {
            binding.recyclerSeriesMejorValoradas.adapter = SerieAdapter(it)
        }

        viewModel.cargando.observe(viewLifecycleOwner) {
            binding.swipeRefreshSeries.isRefreshing = it
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, getString(it.mensaje()), Toast.LENGTH_SHORT).show()
                viewModel.errorMostrado()
            }
        }

        binding.swipeRefreshSeries.setOnRefreshListener { viewModel.cargar() }

        viewModel.cargar()
    }

    private fun prepararLista(lista: androidx.recyclerview.widget.RecyclerView) {
        lista.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        lista.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
