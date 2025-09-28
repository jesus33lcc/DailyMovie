package com.example.dailymovie.activities.views

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
import com.example.dailymovie.graphics.DeslizarParaBorrar
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.activities.viewmodels.MovieViewModel
import com.example.dailymovie.utils.Avisos

class ListMoviesA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityListMoviesBinding
    private lateinit var movieListAdapter: CustomMovieListAdapter
    private lateinit var listName: String
    private var movieList: MutableList<MovieModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listName = intent.getStringExtra("LIST_NAME") ?: ""
        binding.listTitle.text = listName

        movieList = intent.getParcelableArrayListExtra<MovieModel>("MOVIE_LIST")?.toMutableList()
            ?: mutableListOf()

        // Antes habia un if/else que construia dos veces exactamente el mismo adapter, uno
        // para las listas fijas y otro para las del usuario. Se comportan igual, asi que es
        // el mismo: quien sabe distinguirlas es el ViewModel al guardar.
        movieListAdapter = CustomMovieListAdapter(
            onMovieClick = { movie ->
                startActivity(Intent(this, MovieA::class.java).putExtra("MOVIE_ID", movie.id))
            },
            onMovieDelete = { movie, position -> quitarConDeshacer(movie, position) }
        )

        binding.recyclerViewMovies.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMovies.adapter = movieListAdapter
        binding.recyclerViewMovies.addItemDecoration(SpacingItemDecoration.deLista(this))
        movieListAdapter.submitList(movieList.toList())

        setupSwipeToDelete(binding.recyclerViewMovies)
    }

    override fun onResume() {
        super.onResume()
        // Al volver de la ficha la pelicula puede haber dejado de ser favorita o vista, asi
        // que la lista se vuelve a pedir en vez de quedarse con la que llego en el Intent.
        // Antes solo se refrescaban Favoritos y Vistos; las listas del usuario no.
        movieViewModel.cargarLista(listName) { actualizadas ->
            movieList = actualizadas.toMutableList()
            movieListAdapter.submitList(actualizadas)
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val deslizar = DeslizarParaBorrar(this) { posicion ->
            quitarConDeshacer(movieListAdapter.peliculaEn(posicion), posicion)
        }
        ItemTouchHelper(deslizar).attachToRecyclerView(recyclerView)
    }

    /**
     * Quita la pelicula de la lista dando unos segundos para arrepentirse.
     *
     * Antes salia un dialogo preguntando si estabas seguro, que era un paso de mas para algo
     * que se hace a menudo. Ahora la fila desaparece al momento y el aviso de abajo deja
     * deshacerlo; hasta que ese aviso no se va, no se toca Firebase.
     */
    private fun quitarConDeshacer(movie: MovieModel, position: Int) {
        movieList.remove(movie)
        movieListAdapter.submitList(movieList.toList())

        Avisos.conDeshacer(
            vista = binding.root,
            texto = "${movie.title} fuera de $listName",
            alDeshacer = {
                movieList.add(position.coerceAtMost(movieList.size), movie)
                movieListAdapter.submitList(movieList.toList())
            },
            alConfirmar = { quitarDeVerdad(movie, position) }
        )
    }

    /**
     * El borrado que llega a Firebase.
     *
     * Antes esto eran tres bloques calcados, uno por tipo de lista, que se diferenciaban
     * solo en a que funcion llamaban. El ViewModel ya sabe elegir con el enum ListaFija.
     */
    private fun quitarDeVerdad(movie: MovieModel, position: Int) {
        movieViewModel.removeMovieFromList(listName, movie) { bien ->
            if (!bien) {
                // No se pudo guardar, asi que la pelicula vuelve a su sitio: dejarla fuera
                // seria mentirle al usuario, porque al volver a entrar reapareceria.
                Toast.makeText(this, "No se ha podido quitar", Toast.LENGTH_SHORT).show()
                movieList.add(position.coerceAtMost(movieList.size), movie)
                movieListAdapter.submitList(movieList.toList())
            }
        }
    }
}
