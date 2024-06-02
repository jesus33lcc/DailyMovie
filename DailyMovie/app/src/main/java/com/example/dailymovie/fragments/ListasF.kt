package com.example.dailymovie.fragments

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
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.views.ListMoviesA
import com.example.dailymovie.viewmodels.ListasViewModel

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

        binding.misListasCheckFav.layoutManager = LinearLayoutManager(context)
        binding.misListaPersonalizadas.layoutManager = LinearLayoutManager(context)

        viewModel.favoriteAndWatchedLists.observe(viewLifecycleOwner, Observer { listas ->
            initializeListFillInmborrable(listas.toMutableList())
        })

        viewModel.customLists.observe(viewLifecycleOwner, Observer { customLists ->
            initializeCustomLists(customLists.toMutableList())
        })

        binding.btnNewLista.setOnClickListener {
            showCreateListDialog()
        }

        setupSwipeToDelete(binding.misListaPersonalizadas)
    }

    private fun initializeListFillInmborrable(listaListasFill: MutableList<ListaModel>) {
        movieListAdapter = MovieListAdapter(requireActivity(), listaListasFill) { lista ->
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
        binding.misListasCheckFav.adapter = movieListAdapter
    }

    private fun initializeCustomLists(customLists: MutableList<ListaModel>) {
        customListsAdapter = MovieListAdapter(requireActivity(), customLists) { lista ->
            viewModel.getMoviesFromList(lista.nombre) { movieList ->
                navigateToMovieList(movieList, lista.nombre)
            }
        }
        binding.misListaPersonalizadas.adapter = customListsAdapter
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
                val position = viewHolder.adapterPosition
                val list = customListsAdapter.getList()[position]

                // Mostrar dialogo de confirmación
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Eliminar lista")
                builder.setMessage("¿Estás seguro de que deseas eliminar la lista ${list.nombre}?")
                builder.setPositiveButton("Eliminar") { dialog, which ->
                    // Eliminar lista de Firebase
                    viewModel.deleteCustomList(list.nombre) { success ->
                        if (success) {
                            customListsAdapter.removeItem(position)
                            Toast.makeText(context, "Lista eliminada exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al eliminar la lista", Toast.LENGTH_SHORT).show()
                            customListsAdapter.notifyItemChanged(position) // Restaurar elemento si la eliminación falla
                        }
                    }
                }
                builder.setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                    customListsAdapter.notifyItemChanged(position) // Restaurar elemento si se cancela
                }
                builder.show()
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
