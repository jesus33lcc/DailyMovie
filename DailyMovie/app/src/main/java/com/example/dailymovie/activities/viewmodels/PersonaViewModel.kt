package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Filmografia
import com.example.dailymovie.data.PersonRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.models.PersonModel
import com.example.dailymovie.utils.ErrorCarga

class PersonaViewModel(
    private val personas: PersonRepository = Dependencias.personas
) : ViewModel() {

    private val _ficha = MutableLiveData<PersonModel>()
    val ficha: LiveData<PersonModel> get() = _ficha

    private val _filmografia = MutableLiveData<Filmografia>()
    val filmografia: LiveData<Filmografia> get() = _filmografia

    /** Las fotos de la persona. Llega vacio si TMDB no tiene ninguna. */
    private val _fotos = MutableLiveData<List<String>>(emptyList())
    val fotos: LiveData<List<String>> get() = _fotos

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    fun cargar(personaId: Int) {
        personas.ficha(personaId) { resultado ->
            when (resultado) {
                is Resultado.Exito -> _ficha.value = resultado.datos
                is Resultado.Fallo -> _error.value = resultado.motivo
            }
        }
        personas.filmografia(personaId) { resultado ->
            if (resultado is Resultado.Exito) _filmografia.value = resultado.datos
        }
        // Las fotos no cuentan como error si fallan: la ficha se ve igual sin ellas.
        personas.fotos(personaId) { _fotos.value = it }
    }

    fun errorMostrado() {
        _error.value = null
    }
}
