package com.example.dailymovie.fragments.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.activities.views.SerieA
import com.example.dailymovie.activities.views.MovieA
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.databinding.FragmentSeriesBinding
import com.example.dailymovie.fragments.viewmodels.SeriesViewModel
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

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
            pintarTira(binding.recyclerEnEmision, it.map { serie -> Hallazgo.de(serie) })
        }
        viewModel.populares.observe(viewLifecycleOwner) {
            pintarTira(binding.recyclerSeriesPopulares, it.map { serie -> Hallazgo.de(serie) })
        }
        viewModel.mejorValoradas.observe(viewLifecycleOwner) {
            pintarTira(binding.recyclerSeriesMejorValoradas, it.map { serie -> Hallazgo.de(serie) })
        }

        viewModel.cargando.observe(viewLifecycleOwner) {
            binding.swipeRefreshSeries.isRefreshing = it
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
                viewModel.errorMostrado()
            }
        }

        binding.swipeRefreshSeries.setOnRefreshListener { viewModel.cargar() }

        viewModel.cargar()
    }


    /**
     * Pinta una tira con la tarjeta comun.
     *
     * Es la misma que usa el buscador: cartel, titulo, año y nota, con la etiqueta de si es
     * pelicula o serie y el aviso de "Proximamente" en lo que aun no ha salido. Antes cada
     * pantalla tenia su propio adaptador haciendo lo mismo con otro diseño.
     */
    private fun pintarTira(lista: RecyclerView, hallazgos: List<Hallazgo>) {
        if (lista.itemDecorationCount == 0) {
            lista.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
        }
        val adaptador = lista.adapter as? HallazgoAdapter
            ?: HallazgoAdapter { abrirFicha(it) }.also { lista.adapter = it }
        adaptador.submitList(hallazgos)
    }

    private fun abrirFicha(hallazgo: Hallazgo) {
        val destino = when (hallazgo.tipo) {
            TipoDeHallazgo.SERIE ->
                Intent(context, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, hallazgo.id)
            else ->
                Intent(context, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, hallazgo.id)
        }
        startActivity(destino)
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
