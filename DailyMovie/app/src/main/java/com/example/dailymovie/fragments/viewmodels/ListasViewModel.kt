package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.AltaDeLista
import com.example.dailymovie.data.ListaFija
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.ListaModel
import com.example.dailymovie.models.MovieModel

class ListasViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _favoriteAndWatchedLists = MutableLiveData<List<ListaModel>>()
    val favoriteAndWatchedLists: LiveData<List<ListaModel>> get() = _favoriteAndWatchedLists

    private val _customLists = MutableLiveData<List<ListaModel>>()
    val customLists: LiveData<List<ListaModel>> get() = _customLists

    init {
        cargarListasFijas()
        cargarListasDelUsuario()
    }

    private fun cargarListasFijas() {
        _favoriteAndWatchedLists.value = listOf(
            ListaModel(ListaFija.FAVORITOS.titulo, R.drawable.ic_baseline_favorite_24),
            ListaModel(ListaFija.VISTOS.titulo, R.drawable.ic_baseline_visibility_24)
        )
        // Los numeros se piden aparte y en cuanto llegan se vuelve a publicar la lista. Asi
        // los nombres salen al momento y el contador aparece un instante despues, en vez de
        // tener la pantalla en blanco esperando a dos consultas mas.
        usuario.favoritas { favoritas ->
            usuario.vistas { vistas ->
                _favoriteAndWatchedLists.value = listOf(
                    ListaModel(ListaFija.FAVORITOS.titulo, R.drawable.ic_baseline_favorite_24, favoritas.size),
                    ListaModel(ListaFija.VISTOS.titulo, R.drawable.ic_baseline_visibility_24, vistas.size)
                )
            }
        }
    }

    private fun cargarListasDelUsuario() {
        usuario.listasDelUsuario { nombres ->
            _customLists.value = nombres.map { ListaModel(it, R.drawable.ic_baseline_list_24) }
            contarListasDelUsuario(nombres)
        }
    }

    /**
     * Pone el numero de peliculas a cada lista del usuario.
     *
     * Cada lista es un documento aparte en Firestore, asi que hay que pedirlas una a una.
     * Se publica cuando han contestado todas para no repintar la pantalla N veces.
     */
    private fun contarListasDelUsuario(nombres: List<String>) {
        if (nombres.isEmpty()) return
        val cuentas = mutableMapOf<String, Int>()
        nombres.forEach { nombre ->
            usuario.peliculasDeLista(nombre) { peliculas ->
                cuentas[nombre] = peliculas.size
                if (cuentas.size == nombres.size) {
                    _customLists.value = nombres.map {
                        ListaModel(it, R.drawable.ic_baseline_list_24, cuentas[it])
                    }
                }
            }
        }
    }

    /**
     * Crea una lista y devuelve como ha ido.
     *
     * El motivo llega entero hasta la pantalla, que es quien elige el texto. Antes se
     * devolvia un si o no y cualquier fallo se enseñaba como "la lista ya existe".
     */
    fun createNewList(nombre: String, alTerminar: (AltaDeLista) -> Unit) {
        usuario.crearLista(nombre) { resultado ->
            if (resultado == AltaDeLista.CREADA) cargarListasDelUsuario()
            alTerminar(resultado)
        }
    }

    fun getFavorites(alTerminar: (List<MovieModel>) -> Unit) = usuario.favoritas(alTerminar)

    fun getWatched(alTerminar: (List<MovieModel>) -> Unit) = usuario.vistas(alTerminar)

    fun getMoviesFromList(nombre: String, alTerminar: (List<MovieModel>) -> Unit) =
        usuario.peliculasDeLista(nombre, alTerminar)

    /** Carga la lista que toque sin que la pantalla tenga que saber de que tipo es. */
    fun cargarLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit) {
        when (ListaFija.desdeTitulo(nombre)) {
            ListaFija.FAVORITOS -> getFavorites(alTerminar)
            ListaFija.VISTOS -> getWatched(alTerminar)
            null -> getMoviesFromList(nombre, alTerminar)
        }
    }

    fun deleteCustomList(nombre: String, alTerminar: (Boolean) -> Unit) {
        usuario.borrarLista(nombre) { borrada ->
            if (borrada) cargarListasDelUsuario()
            alTerminar(borrada)
        }
    }

}
