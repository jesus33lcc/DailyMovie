package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.R
import com.example.dailymovie.data.Dependencias
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

    /** Motivo por el que no se ha podido crear una lista, para avisar al usuario. */
    private val _avisoCrearLista = MutableLiveData<String?>()
    val avisoCrearLista: LiveData<String?> get() = _avisoCrearLista

    init {
        cargarListasFijas()
        cargarListasDelUsuario()
    }

    private fun cargarListasFijas() {
        _favoriteAndWatchedLists.value = listOf(
            ListaModel(ListaFija.FAVORITOS.titulo, R.drawable.ic_baseline_favorite_24),
            ListaModel(ListaFija.VISTOS.titulo, R.drawable.ic_baseline_visibility_24)
        )
    }

    private fun cargarListasDelUsuario() {
        usuario.listasDelUsuario { nombres ->
            _customLists.value = nombres.map { ListaModel(it, R.drawable.ic_baseline_list_24) }
        }
    }

    fun createNewList(nombre: String, alTerminar: (Boolean) -> Unit) {
        // Las listas de serie no se pueden repetir: antes se comparaba el texto a mano en
        // varios sitios y una lista llamada "Favoritos" chocaba con la de verdad.
        if (ListaFija.estaReservado(nombre)) {
            _avisoCrearLista.value = "Ya existe una lista \"$nombre\", elige otro nombre"
            alTerminar(false)
            return
        }
        usuario.crearLista(nombre) { creada ->
            if (creada) {
                cargarListasDelUsuario()
            } else {
                _avisoCrearLista.value = "Ya tienes una lista con ese nombre"
            }
            alTerminar(creada)
        }
    }

    fun getFavorites(alTerminar: (List<MovieModel>) -> Unit) = usuario.favoritas(alTerminar)

    fun getWatched(alTerminar: (List<MovieModel>) -> Unit) = usuario.vistas(alTerminar)

    fun getMoviesFromList(nombre: String, alTerminar: (List<MovieModel>) -> Unit) =
        usuario.peliculasDeLista(nombre, alTerminar)

    fun deleteCustomList(nombre: String, alTerminar: (Boolean) -> Unit) {
        usuario.borrarLista(nombre) { borrada ->
            if (borrada) cargarListasDelUsuario()
            alTerminar(borrada)
        }
    }

    fun avisoMostrado() {
        _avisoCrearLista.value = null
    }
}
