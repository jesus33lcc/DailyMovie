package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.ListaFija
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.Guardado
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga

class MovieViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _movieDetails = MutableLiveData<MovieDetailsResponse>()
    val movieDetails: LiveData<MovieDetailsResponse> get() = _movieDetails
    private val _movieProviders = MutableLiveData<ProviderResponse>()
    val movieProviders: LiveData<ProviderResponse> get() = _movieProviders
    private val _movieCredits = MutableLiveData<CreditResponse>()
    val movieCredits: LiveData<CreditResponse> get() = _movieCredits
    private val _movieVideos = MutableLiveData<List<VideoModel>>()
    val movieVideos: LiveData<List<VideoModel>> get() = _movieVideos
    private val _similarMovies = MutableLiveData<List<MovieModel>>()
    val similarMovies: LiveData<List<MovieModel>> get() = _similarMovies

    private val _resenas = MutableLiveData<List<ResenaResponse>>()
    val resenas: LiveData<List<ResenaResponse>> get() = _resenas
    private val _recommendedMovies = MutableLiveData<List<MovieModel>>()
    val recommendedMovies: LiveData<List<MovieModel>> get() = _recommendedMovies

    private val _favorites = MutableLiveData<List<MovieModel>>()
    val favorites: LiveData<List<MovieModel>> get() = _favorites
    private val _watched = MutableLiveData<List<MovieModel>>()
    val watched: LiveData<List<MovieModel>> get() = _watched

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    /**
     * La pelicula que hay abierta.
     *
     * La necesitan los dos marcadores de aqui abajo para saber que guardar. Se rellena en
     * [prepararBotonesDeGuardado] y hasta entonces los botones no hacen nada, que es lo
     * correcto: sin ficha cargada no hay nada que marcar.
     */
    private var peliculaAbierta: MovieModel? = null

    /** El corazon. Ver [Marcado] para por que esto no es un simple booleano. */
    val favorita = Marcado { comoQuedar, alTerminar ->
        peliculaAbierta?.let { usuario.ponerFavorita(it, comoQuedar, alTerminar) }
            ?: alTerminar(false)
    }

    /** El ojo, igual que [favorita]. */
    val vista = Marcado { comoQuedar, alTerminar ->
        peliculaAbierta?.let { usuario.ponerVista(it, comoQuedar, alTerminar) }
            ?: alTerminar(false)
    }

    fun cargarPelicula(peliculaId: Int) {
        // Los datos principales avisan si fallan; las secciones de abajo son un extra y
        // simplemente se ocultan, para no llenar la pantalla de mensajes.
        peliculas.detalles(peliculaId) { resultado ->
            when (resultado) {
                is Resultado.Exito -> _movieDetails.value = resultado.datos
                is Resultado.Fallo -> _error.value = resultado.motivo
            }
        }
        peliculas.plataformas(peliculaId) { siVaBien(it) { datos -> _movieProviders.value = datos } }
        peliculas.reparto(peliculaId) { siVaBien(it) { datos -> _movieCredits.value = datos } }
        peliculas.videos(peliculaId) { siVaBien(it) { datos -> _movieVideos.value = datos } }
        peliculas.similares(peliculaId) { siVaBien(it) { datos -> _similarMovies.value = datos } }
        // Las reseñas no cuentan como error si fallan: la ficha se ve igual de bien sin ellas.
        peliculas.resenas(peliculaId) { resultado ->
            if (resultado is Resultado.Exito) _resenas.value = resultado.datos
        }
        peliculas.recomendadas(peliculaId) { siVaBien(it) { datos -> _recommendedMovies.value = datos } }
    }

    private inline fun <T : Any> siVaBien(resultado: Resultado<T>, bloque: (T) -> Unit) {
        if (resultado is Resultado.Exito) bloque(resultado.datos)
    }

    /**
     * Prepara los dos botones de guardar de la ficha.
     *
     * Se llama una vez al abrir: pregunta a Firestore como esta la pelicula y a partir de ahi
     * manda la memoria. Los toques ya no vuelven a preguntar nada.
     *
     * @param pelicula la pelicula abierta, que es lo que se guarda en Firestore.
     */
    fun prepararBotonesDeGuardado(pelicula: MovieModel) {
        peliculaAbierta = pelicula
        usuario.esFavorita(pelicula.id) { favorita.empezarEn(it) }
        usuario.estaVista(pelicula.id) { vista.empezarEn(it) }

        // Abrir la ficha es "haberla mirado". La seccion "Lo que has mirado antes" de Explorar
        // llevaba desde siempre pintandose vacia porque nadie llamaba a esto: el repositorio,
        // el ViewModel y la pantalla estaban, pero faltaba quien lo disparara.
        usuario.anadirAlHistorial(pelicula) { }
    }

    /** Le da la vuelta a "favorita". El icono cambia ya; guardar es cosa de [Marcado]. */
    fun cambiarFavorita() = favorita.cambiar { _error.value = ErrorCarga.SIN_CONEXION }

    /** Le da la vuelta a "vista". */
    fun cambiarVista() = vista.cambiar { _error.value = ErrorCarga.SIN_CONEXION }

    fun getCustomLists(alTerminar: (List<String>) -> Unit) = usuario.listasDelUsuario(alTerminar)

    fun addMovieToList(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) =
        usuario.anadirALista(nombre, pelicula, alTerminar)

    /** En que listas esta ya la pelicula, para abrir el dialogo con las casillas puestas. */
    fun listasConLaPelicula(peliculaId: Int, alTerminar: (Set<String>) -> Unit) =
        usuario.listasQueContienen(peliculaId, alTerminar)

    /** Quitar de una lista fija es cambiar el estado; de una del usuario, sacarla. */
    fun removeMovieFromList(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        when (ListaFija.desdeTitulo(nombre)) {
            // Desde una lista solo se puede sacar, nunca meter, asi que va false fijo.
            ListaFija.FAVORITOS -> usuario.ponerFavorita(pelicula, false, alTerminar)
            ListaFija.VISTOS -> usuario.ponerVista(pelicula, false, alTerminar)
            null -> usuario.quitarDeLista(nombre, pelicula, alTerminar)
        }
    }

    fun getFavorites(alTerminar: (List<MovieModel>) -> Unit) {
        usuario.favoritas {
            _favorites.value = it
            alTerminar(it)
        }
    }

    fun getWatched(alTerminar: (List<MovieModel>) -> Unit) {
        usuario.vistas {
            _watched.value = it
            alTerminar(it)
        }
    }

    fun getMoviesFromList(nombre: String, alTerminar: (List<MovieModel>) -> Unit) =
        usuario.peliculasDeLista(nombre, alTerminar)

    /** Carga la lista que toque, sea fija o del usuario. */
    fun cargarLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit) {
        when (ListaFija.desdeTitulo(nombre)) {
            ListaFija.FAVORITOS -> getFavorites(alTerminar)
            ListaFija.VISTOS -> getWatched(alTerminar)
            null -> getMoviesFromList(nombre, alTerminar)
        }
    }

    /**
     * Todo lo que hay en una lista: peliculas y series juntas.
     *
     * En Firestore van en campos separados, asi que hay que pedir las dos cosas y unirlas.
     * Se espera a que contesten las dos antes de avisar, o la pantalla parpadearia enseñando
     * primero las peliculas y añadiendo las series despues.
     */
    fun cargarListaCompleta(nombre: String, alTerminar: (List<Guardado>) -> Unit) {
        var peliculas: List<Guardado>? = null
        var series: List<Guardado>? = null

        fun avisarSiEstaTodo() {
            val p = peliculas ?: return
            val s = series ?: return
            alTerminar(p + s)
        }

        cargarLista(nombre) { lista ->
            peliculas = lista.map { Guardado.de(it) }
            avisarSiEstaTodo()
        }
        cargarSeriesDeLista(nombre) { lista ->
            series = lista.map { Guardado.de(it) }
            avisarSiEstaTodo()
        }
    }

    private fun cargarSeriesDeLista(nombre: String, alTerminar: (List<SerieModel>) -> Unit) {
        when (ListaFija.desdeTitulo(nombre)) {
            ListaFija.FAVORITOS -> usuario.seriesFavoritas(alTerminar)
            ListaFija.VISTOS -> usuario.seriesVistas(alTerminar)
            null -> usuario.seriesDeLista(nombre, alTerminar)
        }
    }

    /** Saca de la lista lo que sea, pelicula o serie. */
    fun quitarDeLista(nombre: String, elemento: Guardado, alTerminar: (Boolean) -> Unit) {
        if (!elemento.esSerie) return removeMovieFromList(nombre, elemento.aPelicula(), alTerminar)

        val serie = elemento.aSerie()
        when (ListaFija.desdeTitulo(nombre)) {
            ListaFija.FAVORITOS -> usuario.ponerSerieFavorita(serie, false, alTerminar)
            ListaFija.VISTOS -> usuario.ponerSerieVista(serie, false, alTerminar)
            null -> usuario.quitarSerieDeLista(nombre, serie, alTerminar)
        }
    }

    fun errorMostrado() {
        _error.value = null
    }
}
