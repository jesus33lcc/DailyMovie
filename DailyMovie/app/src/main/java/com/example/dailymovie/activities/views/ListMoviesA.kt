package com.example.dailymovie.activities.views

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.adapters.CustomMovieListAdapter
import com.example.dailymovie.databinding.ActivityListMoviesBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.activities.viewmodels.MovieViewModel

class ListMoviesA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityListMoviesBinding
    private lateinit var movieListAdapter: CustomMovieListAdapter
    private lateinit var listName: String
    private var movieList: ArrayList<MovieModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listName = intent.getStringExtra("LIST_NAME") ?: ""

        binding.listTitle.text = listName

        movieList = intent.getParcelableArrayListExtra("MOVIE_LIST") ?: arrayListOf()
        if (listName == "Favoritos" || listName == "Vistos") {
            movieListAdapter = CustomMovieListAdapter(movieList.toMutableList(), { movie ->
                val intent = Intent(this, MovieA::class.java).apply {
                    putExtra("MOVIE_ID", movie.id)
                }
                startActivity(intent)
            }, { movie, position ->
                showDeleteConfirmationDialog(movie, position)
            })
        } else {
            movieListAdapter = CustomMovieListAdapter(movieList.toMutableList(), { movie ->
                val intent = Intent(this, MovieA::class.java).apply {
                    putExtra("MOVIE_ID", movie.id)
                }
                startActivity(intent)
            }, { movie, position ->
                showDeleteConfirmationDialog(movie, position)
            })
        }

        binding.recyclerViewMovies.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMovies.adapter = movieListAdapter
        binding.recyclerViewMovies.addItemDecoration(SpacingItemDecoration(spacing = 8))

        setupSwipeToDelete(binding.recyclerViewMovies)
    }

    override fun onResume() {
        super.onResume()
        if (listName == "Favoritos") {
            movieViewModel.getFavorites { updatedList ->
                movieList.clear()
                movieList.addAll(updatedList)
                movieListAdapter.notifyDataSetChanged()
            }
        } else if (listName == "Vistos") {
            movieViewModel.getWatched { updatedList ->
                movieList.clear()
                movieList.addAll(updatedList)
                movieListAdapter.notifyDataSetChanged()
            }
        }
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
                val movie = movieListAdapter.getMovieAt(position)
                showDeleteConfirmationDialog(movie, position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun showDeleteConfirmationDialog(movie: MovieModel, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar película")
        builder.setMessage("¿Estás seguro de que deseas eliminar la película ${movie.title} de la lista $listName?")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            if (listName == "Favoritos") {
                movieViewModel.toggleFavorite(movie) { success ->
                    if (success) {
                        movieList.remove(movie)
                        movieListAdapter.removeItem(position)
                        Toast.makeText(this, "Película eliminada de Favoritos", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al eliminar la película", Toast.LENGTH_SHORT).show()
                        movieListAdapter.notifyItemChanged(position)
                    }
                }
            } else if (listName == "Vistos") {
                movieViewModel.toggleWatched(movie) { success ->
                    if (success) {
                        movieList.remove(movie)
                        movieListAdapter.removeItem(position)
                        Toast.makeText(this, "Película eliminada de Vistos", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al eliminar la película", Toast.LENGTH_SHORT).show()
                        movieListAdapter.notifyItemChanged(position)
                    }
                }
            } else {
                movieViewModel.removeMovieFromList(listName, movie) { success ->
                    if (success) {
                        movieList.remove(movie)
                        movieListAdapter.removeItem(position)
                        Toast.makeText(this, "Película eliminada de $listName", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al eliminar la película", Toast.LENGTH_SHORT).show()
                        movieListAdapter.notifyItemChanged(position)
                    }
                }
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            movieListAdapter.notifyItemChanged(position)
        }
        builder.show()
    }
}
