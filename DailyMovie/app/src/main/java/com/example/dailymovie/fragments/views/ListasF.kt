package com.example.dailymovie.fragments.views

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.adapters.MovieListAdapter
import com.example.dailymovie.databinding.FragmentListasBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.activities.views.ListMoviesA
import com.example.dailymovie.fragments.viewmodels.ListasViewModel

class ListasF : Fragment() {

    private var _binding: FragmentListasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListasViewModel by viewModels()

    private lateinit var movieListAdapter: MovieListAdapter
    private lateinit var customListsAdapter: MovieListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prepararListas()

        // Los observadores ya solo pasan los datos. Antes cada emision volvia a crear el
        // adapter y a añadir otra decoracion, asi que la separacion entre filas crecia cada
        // vez que se creaba o se borraba una lista.
        viewModel.favoriteAndWatchedLists.observe(viewLifecycleOwner, Observer { listas ->
            movieListAdapter.submitList(listas)
        })

        viewModel.customLists.observe(viewLifecycleOwner, Observer { customLists ->
            customListsAdapter.submitList(customLists)
        })

        binding.btnNewLista.setOnClickListener {
            showCreateListDialog()
        }

        setupSwipeToDelete(binding.misListaPersonalizadas)
    }

    /** Adaptadores y decoraciones, una sola vez. */
    private fun prepararListas() {
        movieListAdapter = MovieListAdapter { lista -> abrirLista(lista) }
        binding.misListasCheckFav.layoutManager = LinearLayoutManager(context)
        binding.misListasCheckFav.adapter = movieListAdapter
        binding.misListasCheckFav.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))

        customListsAdapter = MovieListAdapter { lista -> abrirLista(lista) }
        binding.misListaPersonalizadas.layoutManager = LinearLayoutManager(context)
        binding.misListaPersonalizadas.adapter = customListsAdapter
        binding.misListaPersonalizadas.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
    }

    /**
     * Abre cualquier lista, sea de las fijas o del usuario.
     *
     * Antes habia dos caminos distintos y el de las fijas comparaba el nombre por texto;
     * el ViewModel ya sabe cual toca gracias al enum ListaFija.
     */
    private fun abrirLista(lista: ListaModel) {
        viewModel.cargarLista(lista.nombre) { peliculas ->
            navigateToMovieList(peliculas, lista.nombre)
        }
    }

    private fun navigateToMovieList(movieList: List<MovieModel>, listName: String) {
        val intent = Intent(context, ListMoviesA::class.java)
        intent.putParcelableArrayListExtra("MOVIE_LIST", ArrayList(movieList))
        intent.putExtra("LIST_NAME", listName)
        startActivity(intent)
    }

    private fun showCreateListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Crear nueva lista")

        val input = EditText(requireContext())
        input.hint = "Nombre de la lista"
        builder.setView(input)

        builder.setPositiveButton("Crear") { dialog, which ->
            val listName = input.text.toString().trim()
            if (listName.isNotEmpty()) {
                viewModel.createNewList(listName) { success ->
                    if (success) {
                        Toast.makeText(context, "Lista creada exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "La lista ya existe", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "El nombre de la lista no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val lista = customListsAdapter.listaEn(position)

                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar lista")
                    .setMessage("¿Seguro que quieres eliminar la lista ${lista.nombre}?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deleteCustomList(lista.nombre) { borrada ->
                            if (borrada) {
                                // No hay que sacar la fila a mano: al borrarla el ViewModel
                                // recarga las listas y el comparador ve que falta esa.
                                Toast.makeText(context, "Lista eliminada", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No se ha podido eliminar", Toast.LENGTH_SHORT).show()
                                customListsAdapter.notifyItemChanged(position)
                            }
                        }
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                        // La fila se ha quedado deslizada; hay que devolverla a su sitio.
                        customListsAdapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
