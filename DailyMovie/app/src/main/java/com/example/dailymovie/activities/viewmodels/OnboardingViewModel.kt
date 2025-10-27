package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Gustos
import com.example.dailymovie.client.response.PersonaPopular
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.PersonRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.MovieModel

/**
 * Lleva las tres preguntas del onboarding: generos, peliculas y gente del cine.
 *
 * Las peliculas que se ofrecen no salen de una lista fija: se piden a TMDB segun los
 * generos que acaba de elegir el usuario, asi la segunda pregunta ya va con lo suyo.
 */
class OnboardingViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario,
    private val personas: PersonRepository = Dependencias.personas
) : ViewModel() {

    private val _generos = MutableLiveData<List<GenreModel>>()
    val generos: LiveData<List<GenreModel>> get() = _generos

    private val _peliculasSugeridas = MutableLiveData<List<MovieModel>>()
    val peliculasSugeridas: LiveData<List<MovieModel>> get() = _peliculasSugeridas

    private val _personasSugeridas = MutableLiveData<List<PersonaPopular>>()
    val personasSugeridas: LiveData<List<PersonaPopular>> get() = _personasSugeridas

    private val _guardado = MutableLiveData<Boolean?>()
    val guardado: LiveData<Boolean?> get() = _guardado

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    private val generosElegidos = mutableSetOf<Int>()
    private val peliculasElegidas = mutableSetOf<Int>()
    private val personasElegidas = mutableSetOf<Int>()

    fun cargarGeneros() {
        _cargando.value = true
        peliculas.generos { resultado ->
            _cargando.value = false
            if (resultado is Resultado.Exito) _generos.value = resultado.datos
        }
    }

    fun marcarGenero(id: Int, elegido: Boolean) {
        if (elegido) generosElegidos.add(id) else generosElegidos.remove(id)
    }

    fun generoEstaElegido(id: Int) = id in generosElegidos

    fun hayAlgunGenero() = generosElegidos.isNotEmpty()

    /** Con los generos ya elegidos se buscan peliculas que le puedan sonar al usuario. */
    fun cargarPeliculasSugeridas() {
        _cargando.value = true
        val alRecibir: (Resultado<List<MovieModel>>) -> Unit = { resultado ->
            _cargando.value = false
            if (resultado is Resultado.Exito) _peliculasSugeridas.value = resultado.datos
        }
        if (generosElegidos.isEmpty()) {
            peliculas.populares(alRecibir)
        } else {
            peliculas.porGeneros(generosElegidos.toList(), alTerminar = alRecibir)
        }
    }

    fun marcarPelicula(id: Int, elegida: Boolean) {
        if (elegida) peliculasElegidas.add(id) else peliculasElegidas.remove(id)
    }

    fun peliculaEstaElegida(id: Int) = id in peliculasElegidas

    /**
     * La gente que se ofrece en el tercer paso.
     *
     * Primero se mira quien sale en las peliculas que el usuario acaba de marcar: si ha
     * elegido tres de Nolan, lo suyo es proponerle a Nolan y no al actor de moda de esta
     * semana. Solo cuando no ha marcado ninguna se tira de los populares de TMDB.
     */
    fun cargarPersonasSugeridas() {
        _cargando.value = true
        if (peliculasElegidas.isEmpty()) {
            personas.populares { resultado ->
                _cargando.value = false
                if (resultado is Resultado.Exito) _personasSugeridas.value = resultado.datos
            }
            return
        }

        // Se piden los creditos de unas pocas: con mas de cuatro peticiones la pantalla
        // tardaria demasiado y las caras se repetirian igualmente.
        val aConsultar = peliculasElegidas.take(4)
        val encontradas = linkedMapOf<Int, PersonaPopular>()
        var pendientes = aConsultar.size

        aConsultar.forEach { peliculaId ->
            peliculas.reparto(peliculaId) { resultado ->
                if (resultado is Resultado.Exito) {
                    resultado.datos.cast.take(6)
                        .filter { !it.profilePath.isNullOrBlank() }
                        .forEach {
                            encontradas.putIfAbsent(
                                it.id,
                                PersonaPopular(it.id, it.name, it.profilePath, it.knownForDepartment)
                            )
                        }
                    resultado.datos.crew
                        .filter { it.job == "Director" && !it.profilePath.isNullOrBlank() }
                        .forEach {
                            encontradas.putIfAbsent(
                                it.id,
                                PersonaPopular(it.id, it.name, it.profilePath, "Directing")
                            )
                        }
                }
                pendientes--
                if (pendientes == 0) {
                    _cargando.value = false
                    _personasSugeridas.value = encontradas.values.toList()
                }
            }
        }
    }

    fun marcarPersona(id: Int, elegida: Boolean) {
        if (elegida) personasElegidas.add(id) else personasElegidas.remove(id)
    }

    fun personaEstaElegida(id: Int) = id in personasElegidas

    /**
     * Guarda los gustos. Se puede llamar aunque no haya elegido nada: en ese caso la
     * portada tirara de lo popular y el usuario podra volver a esto desde Ajustes.
     */
    fun guardar() {
        _cargando.value = true
        val gustos = Gustos(
            generos = generosElegidos.toList(),
            peliculas = peliculasElegidas.toList(),
            personas = personasElegidas.toList()
        )
        usuario.guardarGustos(gustos) { bien ->
            _cargando.value = false
            _guardado.value = bien
        }
    }

    /** Para precargar la pantalla si el usuario vuelve a editarlos desde Ajustes. */
    fun cargarGustosGuardados(alTerminar: () -> Unit) {
        usuario.gustos { gustos ->
            gustos?.let {
                generosElegidos.addAll(it.generos)
                peliculasElegidas.addAll(it.peliculas)
                personasElegidas.addAll(it.personas)
            }
            alTerminar()
        }
    }
}
