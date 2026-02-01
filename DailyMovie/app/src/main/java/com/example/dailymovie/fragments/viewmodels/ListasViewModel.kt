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

    // Sin init a proposito: la vista llama a recargar() en cada onResume, y el primero llega
    // justo despues de crear el ViewModel. Con los dos, al entrar en la pestaña se pedian dos
    // veces las favoritas, las vistas, las listas y el contenido de cada lista: con seis
    // listas eran 28 lecturas de Firestore en vez de 14.

    /**
     * Vuelve a pedirlo todo.
     *
     * Hace falta al volver a la pestaña: si el usuario se ha ido a una ficha y ha guardado
     * algo, los numeros de aqui se han quedado viejos. El ViewModel sobrevive al cambio de
     * pestaña, asi que el init no basta.
     */
    fun recargar() {
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
     * Pone a cada lista del usuario cuantas cosas tiene guardadas.
     *
     * Se suman peliculas y series: en Firestore van en dos campos distintos del mismo
     * documento, pero para el usuario una lista es una lista y da igual lo que haya metido
     * dentro. Antes solo se contaban las peliculas, asi que guardar una serie no movia el
     * numero y parecia que no se habia guardado.
     *
     * Cada lista es un documento aparte, asi que hay que pedirlas una a una; se publica
     * cuando han contestado todas para no repintar la pantalla N veces.
     */
    private fun contarListasDelUsuario(nombres: List<String>) {
        if (nombres.isEmpty()) return
        val cuentas = mutableMapOf<String, Int>()

        fun publicarSiEstanTodas() {
            if (cuentas.size < nombres.size) return
            _customLists.value = nombres.map {
                ListaModel(it, R.drawable.ic_baseline_list_24, cuentas[it])
            }
        }

        nombres.forEach { nombre ->
            usuario.peliculasDeLista(nombre) { peliculas ->
                usuario.seriesDeLista(nombre) { series ->
                    cuentas[nombre] = peliculas.size + series.size
                    publicarSiEstanTodas()
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
