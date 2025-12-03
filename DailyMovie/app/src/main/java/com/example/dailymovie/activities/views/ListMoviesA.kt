package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.CustomMovieListAdapter
import com.example.dailymovie.databinding.ActivityListMoviesBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.data.ListaFija
import com.example.dailymovie.models.Guardado
import com.example.dailymovie.activities.viewmodels.MovieViewModel
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.Constantes

class ListMoviesA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityListMoviesBinding
    private lateinit var movieListAdapter: CustomMovieListAdapter
    private lateinit var listName: String
    private var movieList: MutableList<Guardado> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListMoviesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listName = intent.getStringExtra(EXTRA_NOMBRE) ?: ""
        binding.listTitle.text = listName
        binding.btnVolverLista.setOnClickListener { finish() }

        // Lo que llega en el Intent son solo peliculas; las series entran en onResume, que es
        // quien pide la lista completa.
        movieList = intent.getParcelableArrayListExtra<com.example.dailymovie.models.MovieModel>(EXTRA_LISTA)
            ?.map { Guardado.de(it) }?.toMutableList() ?: mutableListOf()

        movieListAdapter = CustomMovieListAdapter(
            onMovieClick = { elemento -> abrirFicha(elemento) },
            onOpciones = { elemento, boton, posicion -> mostrarOpciones(elemento, boton, posicion) }
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
        movieViewModel.cargarListaCompleta(listName) { actualizadas ->
            movieList = actualizadas.toMutableList()
            movieListAdapter.submitList(actualizadas) { decidirSiHayAlgo() }
            pintarCuantasHay(actualizadas)
        }
    }

    /**
     * El "12 películas" de debajo del titulo, que no se enseña si la lista esta vacia.
     *
     * Si dentro hay alguna serie se dice "títulos": una lista con dos series no tiene "2
     * películas", y llamarlas peliculas a todas suena a que la app no se entera.
     */
    private fun pintarCuantasHay(guardados: List<Guardado>) {
        val cuantas = guardados.size
        binding.listCuantas.visibility = if (cuantas == 0) View.GONE else View.VISIBLE
        binding.listCuantas.text = when {
            guardados.any { it.esSerie } -> if (cuantas == 1) "1 título" else "$cuantas títulos"
            cuantas == 1 -> "1 película"
            else -> "$cuantas películas"
        }
    }

    /**
     * Enseña la lista o el aviso de que esta vacia.
     *
     * El texto y el icono cambian segun de que lista se trate: en Favoritos lo util es decir
     * donde esta el corazon, y en una lista propia, que se llena desde la ficha.
     */
    private fun decidirSiHayAlgo() {
        val vacia = movieList.isEmpty()
        binding.panelListaVacia.visibility = if (vacia) View.VISIBLE else View.GONE
        binding.recyclerViewMovies.visibility = if (vacia) View.GONE else View.VISIBLE
        if (!vacia) return

        val (texto, icono) = when (ListaFija.desdeTitulo(listName)) {
            ListaFija.FAVORITOS ->
                R.string.lista_vacia_favoritos to R.drawable.ic_baseline_favorite_24
            ListaFija.VISTOS ->
                R.string.lista_vacia_vistos to R.drawable.ic_baseline_visibility_24
            null ->
                R.string.lista_vacia_propia to R.drawable.ic_baseline_list_24
        }
        binding.txtListaVacia.setText(texto)
        binding.imgListaVacia.setImageResource(icono)
    }

    private fun abrirFicha(elemento: Guardado) {
        val destino = if (elemento.esSerie) {
            Intent(this, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, elemento.id)
        } else {
            Intent(this, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, elemento.id)
        }
        startActivity(destino)
    }

    /**
     * El menu de los tres puntos de cada cartel.
     *
     * Sustituye al borrado deslizando: el gesto no lo descubria nadie y en una cuadricula
     * ademas no se entiende, porque no queda claro que se esta arrastrando. Con el menu
     * caben otras acciones sin inventar mas gestos.
     */
    private fun mostrarOpciones(elemento: Guardado, boton: View, posicion: Int) {
        PopupMenu(ContextThemeWrapper(this, R.style.TemaPopupDailyMovie), boton).apply {
            inflate(R.menu.menu_pelicula_de_lista)
            setOnMenuItemClickListener { opcion ->
                when (opcion.itemId) {
                    R.id.accion_ver_ficha -> abrirFicha(elemento)
                    R.id.accion_compartir -> compartir(elemento)
                    R.id.accion_quitar -> quitarConDeshacer(elemento, posicion)
                }
                true
            }
            show()
        }
    }

    private fun compartir(elemento: Guardado) {
        val direccion = if (elemento.esSerie) Constantes.BASE_SERIE_URL else Constantes.BASE_MOVIE_URL
        val queEs = if (elemento.esSerie) "esta serie" else "esta película"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Te recomiendo $queEs: ${elemento.titulo}\n$direccion${elemento.id}")
        }
        startActivity(Intent.createChooser(intent, "Compartir"))
    }

    /**
     * Quita la pelicula dando unos segundos para arrepentirse.
     *
     * La fila desaparece al momento y el aviso de abajo deja deshacerlo; hasta que ese aviso
     * no se va, no se toca Firebase.
     */
    private fun quitarConDeshacer(elemento: Guardado, position: Int) {
        movieList.remove(elemento)
        movieListAdapter.submitList(movieList.toList()) { decidirSiHayAlgo() }

        Avisos.conDeshacer(
            vista = binding.root,
            texto = "${elemento.titulo} fuera de $listName",
            alDeshacer = {
                movieList.add(position.coerceAtMost(movieList.size), elemento)
                movieListAdapter.submitList(movieList.toList())
            },
            alConfirmar = { quitarDeVerdad(elemento, position) }
        )
    }

    /**
     * El borrado que llega a Firebase.
     *
     * Antes esto eran tres bloques calcados, uno por tipo de lista, que se diferenciaban
     * solo en a que funcion llamaban. El ViewModel ya sabe elegir con el enum ListaFija.
     */
    private fun quitarDeVerdad(elemento: Guardado, position: Int) {
        movieViewModel.quitarDeLista(listName, elemento) { bien ->
            if (!bien) {
                // No se pudo guardar, asi que la pelicula vuelve a su sitio: dejarla fuera
                // seria mentirle al usuario, porque al volver a entrar reapareceria.
                Avisos.breve(binding.root, "No se ha podido quitar")
                movieList.add(position.coerceAtMost(movieList.size), elemento)
                movieListAdapter.submitList(movieList.toList())
            }
        }
    }

    companion object {
        /** Lo que ya se sabe de la lista, para pintar algo antes de que conteste Firebase. */
        const val EXTRA_LISTA = "MOVIE_LIST"

        /** El nombre de la lista, que ademas es su id en Firestore. */
        const val EXTRA_NOMBRE = "LIST_NAME"
    }
}
