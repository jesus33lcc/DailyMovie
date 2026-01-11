package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.utils.ErrorCarga

/**
 * Las películas de una saga.
 *
 * `SagaA` era la única pantalla sin ViewModel: hablaba con el repositorio desde `onCreate`.
 * Eso quería decir que al girar el móvil volvía a pedírselo todo a TMDB y que no se podía
 * probar. Con esto se comporta como el resto y las películas sobreviven al giro.
 *
 * @param peliculas de dónde salen. Por defecto las de verdad; en un test se le pasa un doble.
 */
class SagaViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas
) : ViewModel() {

    private val _peliculas = MutableLiveData<List<Hallazgo>>()

    /** Las de la saga, en orden de estreno. */
    val peliculasDeLaSaga: LiveData<List<Hallazgo>> get() = _peliculas

    private val _cargando = MutableLiveData(true)
    val cargando: LiveData<Boolean> get() = _cargando

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    /** Para no volver a pedirlas al girar el móvil. */
    private var yaPedidas = false

    /**
     * Pide las películas de la saga.
     *
     * @param sagaId el id de colección de TMDB.
     */
    fun cargar(sagaId: Int) {
        if (yaPedidas) return
        yaPedidas = true

        peliculas.peliculasDeLaSaga(sagaId) { resultado ->
            _cargando.value = false
            when (resultado) {
                // Por orden de estreno: TMDB no siempre las devuelve ordenadas, y una saga
                // desordenada no sirve de nada.
                is Resultado.Exito -> _peliculas.value =
                    resultado.datos.sortedBy { it.releaseDate }.map { Hallazgo.de(it) }
                is Resultado.Fallo -> _error.value = resultado.motivo
            }
        }
    }

    fun errorMostrado() {
        _error.value = null
    }
}
