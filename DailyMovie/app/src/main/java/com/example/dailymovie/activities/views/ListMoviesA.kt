package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.CustomMovieListAdapter
import com.example.dailymovie.databinding.ActivityListMoviesBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.activities.viewmodels.MovieViewModel
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.Constantes

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

        movieListAdapter = CustomMovieListAdapter(
            onMovieClick = { movie -> abrirFicha(movie) },
            onOpciones = { movie, boton, posicion -> mostrarOpciones(movie, boton, posicion) }
        )

        binding.recyclerViewMovies.layoutManager = GridLayoutManager(this, columnasQueCaben())
        binding.recyclerViewMovies.adapter = movieListAdapter
        binding.recyclerViewMovies.addItemDecoration(SpacingItemDecoration.deLista(this))
        movieListAdapter.submitList(movieList.toList())
    }

    /**
     * Cuantos carteles caben a lo ancho.
     *
     * Se calcula en vez de fijar un numero porque la misma cuenta vale para un movil en
     * vertical, para el mismo movil girado y para la tablet, donde el cartel ademas es mas
     * grande porque sale de otro dimens.
     */
    private fun columnasQueCaben(): Int {
        val anchoPantalla = resources.displayMetrics.widthPixels
        val anchoCartel = resources.getDimensionPixelSize(R.dimen.poster_ancho)
        val separacion = resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)
        val margenes = resources.getDimensionPixelSize(R.dimen.recycler_view_spacing) * 4
        return ((anchoPantalla - margenes) / (anchoCartel + separacion * 2)).coerceAtLeast(2)
    }

    override fun onResume() {
        super.onResume()
        // Al volver de la ficha la pelicula puede haber dejado de ser favorita o vista, asi
        // que la lista se vuelve a pedir en vez de quedarse con la que llego en el Intent.
        movieViewModel.cargarLista(listName) { actualizadas ->
            movieList = actualizadas.toMutableList()
            movieListAdapter.submitList(actualizadas)
        }
    }

    private fun abrirFicha(movie: MovieModel) {
        startActivity(Intent(this, MovieA::class.java).putExtra("MOVIE_ID", movie.id))
    }

    /**
     * El menu de los tres puntos de cada cartel.
     *
     * Sustituye al borrado deslizando: el gesto no lo descubria nadie y en una cuadricula
     * ademas no se entiende, porque no queda claro que se esta arrastrando. Con el menu
     * caben otras acciones sin inventar mas gestos.
     */
    private fun mostrarOpciones(movie: MovieModel, boton: View, posicion: Int) {
        PopupMenu(ContextThemeWrapper(this, R.style.TemaPopupDailyMovie), boton).apply {
            inflate(R.menu.menu_pelicula_de_lista)
            setOnMenuItemClickListener { opcion ->
                when (opcion.itemId) {
                    R.id.accion_ver_ficha -> abrirFicha(movie)
                    R.id.accion_compartir -> compartir(movie)
                    R.id.accion_quitar -> quitarConDeshacer(movie, posicion)
                }
                true
            }
            show()
        }
    }

    private fun compartir(movie: MovieModel) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Te recomiendo esta película: ${movie.title}\n${Constantes.BASE_MOVIE_URL}${movie.id}"
            )
        }
        startActivity(Intent.createChooser(intent, "Compartir película"))
    }

    /**
     * Quita la pelicula dando unos segundos para arrepentirse.
     *
     * La fila desaparece al momento y el aviso de abajo deja deshacerlo; hasta que ese aviso
     * no se va, no se toca Firebase.
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
