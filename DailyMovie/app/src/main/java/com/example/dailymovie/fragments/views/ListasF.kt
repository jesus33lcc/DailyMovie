package com.example.dailymovie.fragments.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.adapters.MovieListAdapter
import com.example.dailymovie.data.ListaFija
import com.example.dailymovie.databinding.FragmentListasBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.activities.views.ListMoviesA
import com.example.dailymovie.fragments.viewmodels.ListasViewModel
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

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
            // Sin esto, quien entra por primera vez ve un hueco y el boton + sin mas.
            binding.txtSinListas.visibility =
                if (customLists.isEmpty()) View.VISIBLE else View.GONE
        })

        binding.btnNewLista.setOnClickListener {
            showCreateListDialog()
        }

    }

    /** Adaptadores y decoraciones, una sola vez. */
    private fun prepararListas() {
        // Sin menu: Favoritos y Vistos vienen con la app y no se pueden borrar
        movieListAdapter = MovieListAdapter(itemClickListener = { lista -> abrirLista(lista) })
        binding.misListasCheckFav.layoutManager = LinearLayoutManager(context)
        binding.misListasCheckFav.adapter = movieListAdapter
        binding.misListasCheckFav.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))

        customListsAdapter = MovieListAdapter(
            itemClickListener = { lista -> abrirLista(lista) },
            onOpciones = { lista, boton -> mostrarOpcionesDeLista(lista, boton) }
        )
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
        intent.putParcelableArrayListExtra(ListMoviesA.EXTRA_LISTA, ArrayList(movieList))
        intent.putExtra(ListMoviesA.EXTRA_NOMBRE, listName)
        startActivity(intent)
    }

    /**
     * Crear una lista.
     *
     * El aviso sale mientras escribes: si el nombre ya esta cogido o lleva una barra se ve
     * al momento, en vez de dejarte pulsar Crear para luego decirte que no valia.
     */
    private fun showCreateListDialog() {
        val contenido = layoutInflater.inflate(R.layout.dialogo_campo_texto, null)
        val campo = contenido.findViewById<EditText>(R.id.dialogoCampo)
        val aviso = contenido.findViewById<TextView>(R.id.dialogoAviso)
        campo.hint = "Nombre de la lista"

        // Los nombres que ya no estan libres: los de las listas del usuario y los dos fijos.
        val cogidos = (viewModel.customLists.value.orEmpty().map { it.nombre } +
            ListaFija.entries.map { it.titulo }).map { it.lowercase() }

        fun porQueNoVale(texto: String): String? = when {
            texto.isBlank() -> null // aun no ha escrito nada, no hay nada que reprocharle
            texto.lowercase() in cogidos -> getString(R.string.lista_ya_existe)
            texto.contains("/") -> getString(R.string.lista_nombre_invalido)
            else -> null
        }

        campo.doAfterTextChanged { texto ->
            val problema = porQueNoVale(texto.toString().trim())
            aviso.text = problema.orEmpty()
            aviso.visibility = if (problema == null) View.GONE else View.VISIBLE
        }

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = "Crear nueva lista",
            mensaje = "Ponle un nombre y podrás guardar ahí las películas que quieras",
            contenido = contenido,
            textoAceptar = "Crear"
        ) { dialogo ->
            val nombre = campo.text.toString().trim()
            val problema = porQueNoVale(nombre)
            when {
                // El dialogo se queda abierto mientras el nombre no valga, con el aviso
                // puesto: cerrarlo obligaria a volver a escribirlo todo.
                nombre.isBlank() -> {
                    aviso.text = getString(R.string.lista_nombre_vacio)
                    aviso.visibility = View.VISIBLE
                }
                problema != null -> {
                    aviso.text = problema
                    aviso.visibility = View.VISIBLE
                }
                else -> viewModel.createNewList(nombre) { resultado ->
                    dialogo.dismiss()
                    Avisos.breve(binding.root, getString(resultado.mensaje(), nombre))
                }
            }
        }
    }

    /**
     * El menu de los tres puntos de una lista.
     *
     * Antes se borraba deslizando, pero el gesto no lo descubria nadie y ademas chocaba con
     * el scroll de la pantalla. Aqui si se sigue preguntando antes de borrar: una lista se
     * lleva por delante todo lo que hubiera dentro y no basta con poder deshacerlo durante
     * unos segundos, como cuando se quita una sola pelicula.
     */
    private fun mostrarOpcionesDeLista(lista: ListaModel, boton: View) {
        PopupMenu(ContextThemeWrapper(requireContext(), R.style.TemaPopupDailyMovie), boton).apply {
            inflate(R.menu.menu_lista)
            setOnMenuItemClickListener { opcion ->
                when (opcion.itemId) {
                    R.id.accion_abrir_lista -> abrirLista(lista)
                    R.id.accion_eliminar_lista -> confirmarEliminar(lista)
                }
                true
            }
            show()
        }
    }

    private fun confirmarEliminar(lista: ListaModel) {
        DialogoDailyMovie.confirmar(
            context = requireContext(),
            titulo = "Eliminar lista",
            mensaje = "Se borra \"${lista.nombre}\" y las películas que tengas guardadas ahí. " +
                "Esto no se puede deshacer.",
            textoAceptar = "Eliminar",
            peligroso = true
        ) {
            viewModel.deleteCustomList(lista.nombre) { borrada ->
                // No hay que sacar la fila a mano: al borrarla el ViewModel recarga las
                // listas y el comparador ve que falta esa.
                val texto = if (borrada) "Lista eliminada" else "No se ha podido eliminar"
                Avisos.breve(binding.root, texto)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
