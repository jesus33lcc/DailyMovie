package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.SerieRepository
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.models.GeneroExplorable
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.ErrorCarga
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Como se filtran y ordenan los resultados. Un tipo a null es "todo". */
data class FiltroDeBusqueda(
    val tipo: TipoDeHallazgo? = null,
    val orden: OrdenDeBusqueda = OrdenDeBusqueda.RELEVANCIA
)

enum class OrdenDeBusqueda { RELEVANCIA, NOTA, ANO }

class ExplorarViewModel(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val series: SerieRepository = Dependencias.series,
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    /** Lo que devuelve TMDB, sin tocar. Los filtros se aplican encima al enseñarlo. */
    private var todosLosResultados: List<Hallazgo> = emptyList()

    private val _resultados = MutableLiveData<List<Hallazgo>>()
    val resultados: LiveData<List<Hallazgo>> get() = _resultados

    private val _tendencias = MutableLiveData<List<Hallazgo>>()
    val tendencias: LiveData<List<Hallazgo>> get() = _tendencias

    private val _generos = MutableLiveData<List<GeneroExplorable>>()
    val generos: LiveData<List<GeneroExplorable>> get() = _generos

    private val _history = MutableLiveData<List<MovieModel>>()
    val history: LiveData<List<MovieModel>> get() = _history

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    /** True cuando se ha buscado algo y no ha salido nada, para poder decirlo. */
    private val _sinResultados = MutableLiveData(false)
    val sinResultados: LiveData<Boolean> get() = _sinResultados

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    var filtro = FiltroDeBusqueda()
        private set

    /** Los filtros finos: año, nota y plataforma. */
    var filtrosAvanzados = FiltrosAvanzados()
        private set

    private val _plataformas = MutableLiveData<List<PlataformaDisponible>>()
    val plataformas: LiveData<List<PlataformaDisponible>> get() = _plataformas

    private var consultaActual = ""
    private var trabajoDeBusqueda: Job? = null

    /** En que pagina va la busqueda y si detras hay mas. */
    private var paginaActual = 1
    private var hayMasPaginas = false
    private var pidiendoMas = false

    /** El genero que se esta explorando, si es que se llego por ahi y no escribiendo. */
    private var generoEnCurso: GeneroExplorable? = null

    /**
     * Busca con freno.
     *
     * Antes salia una peticion por cada letra a partir de la tercera: escribir "interstellar"
     * eran nueve llamadas a TMDB, y como cada una tarda lo suyo, las respuestas podian llegar
     * desordenadas y pintarse los resultados de "inters" encima de los de "interstellar".
     *
     * Ahora cada tecla cancela la busqueda anterior y se espera un momento a que pares de
     * escribir: sale una sola peticion, y siempre la de lo ultimo que hay en el campo.
     */
    /**
     * De que busqueda es cada respuesta.
     *
     * Cancelar el Job solo sirve mientras la corrutina esta en el `delay`: en cuanto sale la
     * peticion a TMDB, el callback de Retrofit ya no depende de el y llega igual. Si escribes
     * "inters" y luego "interstellar", las dos respuestas estan en el aire y la del texto
     * viejo puede llegar la ultima y pisar a la buena.
     *
     * Cada busqueda se lleva su numero y al contestar se compara: si ya no es la de ahora, la
     * respuesta se tira. Lo mismo vale para los filtros, los generos y la paginacion, que
     * comparten el mismo contador porque cualquiera de ellos invalida a los demas.
     */
    private var generacionDeBusqueda = 0

    /** Empieza una busqueda nueva y devuelve su numero, invalidando todo lo que estuviera en el aire. */
    private fun nuevaGeneracion(): Int {
        pidiendoMas = false
        return ++generacionDeBusqueda
    }

    fun buscar(consulta: String) {
        val limpia = consulta.trim()
        consultaActual = limpia

        trabajoDeBusqueda?.cancel()
        val mia = nuevaGeneracion()

        if (limpia.length < MINIMO_PARA_BUSCAR) {
            todosLosResultados = emptyList()
            _resultados.value = emptyList()
            _sinResultados.value = false
            _cargando.value = false
            return
        }

        generoEnCurso = null
        paginaActual = 1

        trabajoDeBusqueda = viewModelScope.launch {
            delay(ESPERA_ANTES_DE_BUSCAR)
            _cargando.value = true
            peliculas.buscarTodo(limpia, pagina = 1) { resultado ->
                // Si mientras contestaba TMDB el usuario escribio otra cosa, esta respuesta
                // ya no vale para nada.
                if (mia != generacionDeBusqueda) return@buscarTodo
                _cargando.value = false
                when (resultado) {
                    is Resultado.Exito -> {
                        todosLosResultados = resultado.datos.hallazgos
                        hayMasPaginas = resultado.datos.hayMas
                        aplicarFiltro()
                        _sinResultados.value = todosLosResultados.isEmpty()
                    }
                    is Resultado.Fallo -> {
                        todosLosResultados = emptyList()
                        hayMasPaginas = false
                        _resultados.value = emptyList()
                        _sinResultados.value = false
                        _error.value = resultado.motivo
                    }
                }
            }
        }
    }

    /**
     * Pide la siguiente tanda y la añade a lo que ya hay.
     *
     * Hasta ahora solo se pedia la primera pagina, asi que cualquier busqueda se quedaba en
     * veinte resultados y al llegar abajo no pasaba nada. La pantalla llama a esto cuando el
     * usuario se acerca al final de la rejilla.
     *
     * Se protege con `pidiendoMas` porque el scroll dispara muchas veces seguidas y si no se
     * pedirian tres o cuatro paginas iguales a la vez.
     */
    fun cargarMas() {
        if (pidiendoMas || !hayMasPaginas) return
        pidiendoMas = true
        val siguiente = paginaActual + 1
        // La pagina 2 se pide de una busqueda concreta. Si el usuario escribe otra cosa
        // mientras baja, esta respuesta llegaria y se pegaria a los resultados nuevos,
        // dejando la rejilla con una mezcla de las dos busquedas.
        val mia = generacionDeBusqueda

        val alRecibir: (List<Hallazgo>, Boolean) -> Unit = alRecibir@{ nuevos, quedanMas ->
            if (mia != generacionDeBusqueda) return@alRecibir
            pidiendoMas = false
            paginaActual = siguiente
            hayMasPaginas = quedanMas
            // Se filtran los repetidos: TMDB devuelve alguno dos veces entre paginas, y con
            // el mismo id el comparador de la lista se queja.
            val yaEstan = todosLosResultados.map { it.id to it.tipo }.toSet()
            todosLosResultados = todosLosResultados + nuevos.filter { (it.id to it.tipo) !in yaEstan }
            aplicarFiltro()
        }

        // Las paginas siguientes tienen que venir de donde vino la primera. Si no se mira
        // esto, con filtros puestos la pagina 1 llega filtrada por discover y la 2 sin
        // filtrar por search, asi que al bajar aparecian resultados que el filtro habia
        // quitado.
        val genero = generoEnCurso
        when {
            !filtrosAvanzados.estanVacios() -> peliculas.descubrir(
                generos = listOfNotNull(genero?.idPelicula),
                filtros = filtrosAvanzados,
                pagina = siguiente
            ) { resultado ->
                if (mia != generacionDeBusqueda) return@descubrir
                if (resultado is Resultado.Exito) {
                    alRecibir(resultado.datos.map { Hallazgo.de(it) }, resultado.datos.size >= POR_PAGINA)
                } else {
                    pidiendoMas = false
                }
            }
            genero != null -> cargarGeneroEnPagina(genero, siguiente, alRecibir)
            else -> peliculas.buscarTodo(consultaActual, siguiente) { resultado ->
                if (mia != generacionDeBusqueda) return@buscarTodo
                if (resultado is Resultado.Exito) {
                    alRecibir(resultado.datos.hallazgos, resultado.datos.hayMas)
                } else {
                    pidiendoMas = false
                }
            }
        }
    }

    /**
     * Cambia los filtros finos y vuelve a pedir.
     *
     * Con filtros puestos se usa discover en vez de search, porque search no sabe filtrar por
     * año ni por nota ni por plataforma: solo busca texto. La contrapartida es que discover no
     * entiende de titulos, asi que lo escrito en la barra deja de contar mientras haya filtros.
     */
    fun cambiarFiltrosAvanzados(nuevos: FiltrosAvanzados) {
        filtrosAvanzados = nuevos
        paginaActual = 1
        val mia = nuevaGeneracion()

        if (nuevos.estanVacios()) {
            // Sin filtros se vuelve a lo de antes: lo escrito, o el genero que hubiera.
            val genero = generoEnCurso
            when {
                genero != null -> explorarGenero(genero)
                consultaActual.length >= MINIMO_PARA_BUSCAR -> buscar(consultaActual)
                else -> {
                    todosLosResultados = emptyList()
                    aplicarFiltro()
                }
            }
            return
        }

        _cargando.value = true
        peliculas.descubrir(
            generos = listOfNotNull(generoEnCurso?.idPelicula),
            filtros = nuevos,
            pagina = 1
        ) { resultado ->
            if (mia != generacionDeBusqueda) return@descubrir
            _cargando.value = false
            if (resultado is Resultado.Exito) {
                todosLosResultados = resultado.datos.map { Hallazgo.de(it) }
                hayMasPaginas = resultado.datos.size >= POR_PAGINA
                aplicarFiltro()
                _sinResultados.value = todosLosResultados.isEmpty()
            } else if (resultado is Resultado.Fallo) {
                _error.value = resultado.motivo
            }
        }
    }

    fun cargarPlataformas() {
        if (_plataformas.value?.isNotEmpty() == true) return
        peliculas.plataformasDisponibles { resultado ->
            if (resultado is Resultado.Exito) _plataformas.value = resultado.datos
        }
    }

    /** Con filtros puestos, la busqueda por texto no manda: manda discover. */
    fun hayBusquedaOFiltros() = hayBusquedaEnMarcha() || !filtrosAvanzados.estanVacios()

    fun cambiarFiltro(nuevo: FiltroDeBusqueda) {
        filtro = nuevo
        aplicarFiltro()
    }

    /** Los filtros se aplican sobre lo ya descargado: cambiar de chip no vuelve a la red. */
    /** Si de ese resultado se puede enseñar una nota: la gente no tiene, y lo por estrenar tampoco. */
    private fun tieneNotaQueEnseñar(hallazgo: Hallazgo) =
        hallazgo.nota > 0 &&
            hallazgo.tipo != TipoDeHallazgo.PERSONA &&
            hallazgo.tipo != TipoDeHallazgo.SAGA &&
            !Fechas.estaPorVenir(hallazgo.subtitulo)

    private fun aplicarFiltro() {
        val porTipo = filtro.tipo?.let { t -> todosLosResultados.filter { it.tipo == t } }
            ?: todosLosResultados

        _resultados.value = when (filtro.orden) {
            OrdenDeBusqueda.RELEVANCIA -> porTipo
            // Lo que no tiene nota va al final: las sagas y la gente no la tienen, y de lo
            // que aun no ha salido no se enseña. Sin esto, ordenar por nota dejaba arriba
            // justo lo que no la enseñaba, y parecia que el orden no habia hecho nada.
            OrdenDeBusqueda.NOTA -> porTipo.sortedWith(
                compareBy<Hallazgo> { tieneNotaQueEnseñar(it).not() }
                    .thenByDescending { it.nota }
            )
            OrdenDeBusqueda.ANO -> porTipo.sortedByDescending { it.ano }
        }
    }

    /** Cuantos hay de cada tipo, para poder ponerlo en los chips. */
    fun cuantosHayDe(tipo: TipoDeHallazgo?): Int =
        if (tipo == null) todosLosResultados.size
        else todosLosResultados.count { it.tipo == tipo }

    fun hayBusquedaEnMarcha() = consultaActual.length >= MINIMO_PARA_BUSCAR

    // ---- Lo que se enseña cuando no se ha escrito nada ----

    fun cargarTendencias() {
        if (_tendencias.value?.isNotEmpty() == true) return
        peliculas.tendencias { resultado ->
            if (resultado is Resultado.Exito) _tendencias.value = resultado.datos
        }
    }

    /**
     * Junta los generos de cine y los de television en una sola lista de chips.
     *
     * Se esperan las dos respuestas antes de pintar nada: si no, saldrian primero los de
     * cine y los chips se moverian solos al llegar los de series.
     */
    fun cargarGeneros() {
        if (_generos.value?.isNotEmpty() == true) return

        var deCine: List<GenreModel>? = null
        var deSeries: List<GenreModel>? = null

        fun combinarSiEstanLos2() {
            val cine = deCine ?: return
            val series = deSeries ?: return
            _generos.value = combinar(cine, series)
        }

        peliculas.generos { resultado ->
            deCine = (resultado as? Resultado.Exito)?.datos.orEmpty()
            combinarSiEstanLos2()
        }
        this.series.generos { resultado ->
            deSeries = (resultado as? Resultado.Exito)?.datos.orEmpty()
            combinarSiEstanLos2()
        }
    }

    /**
     * Cruza las dos listas de generos.
     *
     * Primero por nombre, que es lo que casa en la mayoria (Comedia, Drama, Crimen...), y
     * para los que TMDB no traduce o agrupa de otra forma se tira del puente de a mano.
     * Los generos que solo existen en series (Reality, Talk Show, Kids) se añaden al final,
     * porque tambien hay que poder explorarlos.
     */
    private fun combinar(cine: List<GenreModel>, series: List<GenreModel>): List<GeneroExplorable> {
        val seriesPorNombre = series.associateBy { it.name.lowercase() }
        val usadosDeSeries = mutableSetOf<Int>()

        val combinados = cine.map { genero ->
            val porNombre = seriesPorNombre[genero.name.lowercase()]?.id
            val porPuente = GeneroExplorable.PUENTE_A_SERIES[genero.name.lowercase()]
            val idSerie = porNombre ?: porPuente
            idSerie?.let { usadosDeSeries.add(it) }
            GeneroExplorable(genero.name, genero.id, idSerie)
        }

        val soloDeSeries = series
            .filter { it.id !in usadosDeSeries }
            .map { GeneroExplorable(it.name, null, it.id) }

        return combinados + soloDeSeries
    }

    /**
     * Enseña lo mas popular de un genero, con peliculas y series juntas.
     *
     * Reaprovecha discover, el mismo endpoint que alimenta la recomendacion de la portada.
     * Los resultados se meten en la misma rejilla, asi que tocar un genero se comporta igual
     * que escribir algo: mismos filtros y mismas tarjetas.
     *
     * Se piden las dos cosas y se espera a las dos antes de pintar. Al principio esto solo
     * traia peliculas, porque discover/movie es lo unico que se estaba usando y las series
     * no aparecian por ningun lado.
     *
     * @param genero el genero elegido, que ya trae dentro el id de cine y el de television.
     */
    fun explorarGenero(genero: GeneroExplorable) {
        trabajoDeBusqueda?.cancel()
        consultaActual = genero.nombre
        generoEnCurso = genero
        paginaActual = 1
        _cargando.value = true
        val mia = nuevaGeneracion()

        cargarGeneroEnPagina(genero, 1) { hallazgos, quedanMas ->
            if (mia != generacionDeBusqueda) return@cargarGeneroEnPagina
            _cargando.value = false
            todosLosResultados = hallazgos
            hayMasPaginas = quedanMas
            aplicarFiltro()
            _sinResultados.value = hallazgos.isEmpty()
        }
    }

    /**
     * Pide una pagina de un genero, en cine y en television a la vez.
     *
     * Se espera a que contesten los dos y se intercalan, para que la rejilla no salga con
     * todas las peliculas primero y las series al final como si fueran dos grupos.
     *
     * @param pagina cual de las tandas de veinte se pide, en los dos sitios.
     * @param alTerminar recibe lo encontrado y si conviene seguir pidiendo. Se da por hecho
     *   que quedan mas mientras cualquiera de los dos siga devolviendo una pagina llena.
     */
    private fun cargarGeneroEnPagina(
        genero: GeneroExplorable,
        pagina: Int,
        alTerminar: (List<Hallazgo>, Boolean) -> Unit
    ) {
        var deCine: List<Hallazgo>? = if (genero.idPelicula == null) emptyList() else null
        var deSeries: List<Hallazgo>? = if (genero.idSerie == null) emptyList() else null

        fun publicarSiEstaTodo() {
            val cine = deCine ?: return
            val series = deSeries ?: return
            val quedanMas = cine.size >= POR_PAGINA || series.size >= POR_PAGINA
            alTerminar(intercalar(cine, series), quedanMas)
        }

        genero.idPelicula?.let { id ->
            peliculas.porGeneros(listOf(id), pagina) { resultado ->
                deCine = (resultado as? Resultado.Exito)?.datos?.map { Hallazgo.de(it) }.orEmpty()
                publicarSiEstaTodo()
            }
        }
        genero.idSerie?.let { id ->
            series.porGeneros(listOf(id), pagina) { resultado ->
                deSeries = (resultado as? Resultado.Exito)?.datos?.map { Hallazgo.de(it) }.orEmpty()
                publicarSiEstaTodo()
            }
        }
        publicarSiEstaTodo()
    }

    /** Uno de cada, para que peliculas y series salgan mezcladas en la rejilla. */
    private fun intercalar(unos: List<Hallazgo>, otros: List<Hallazgo>): List<Hallazgo> {
        val mezcla = mutableListOf<Hallazgo>()
        val maximo = maxOf(unos.size, otros.size)
        for (i in 0 until maximo) {
            unos.getOrNull(i)?.let { mezcla += it }
            otros.getOrNull(i)?.let { mezcla += it }
        }
        return mezcla
    }

    /** Una al azar de lo que esta en tendencia, para el "no se que ver". */
    fun unaAlAzar(): Hallazgo? = _tendencias.value?.randomOrNull()

    fun loadHistory() {
        usuario.historial { _history.value = it }
    }

    fun errorMostrado() {
        _error.value = null
    }

    private companion object {
        /** Con una sola letra cualquier busqueda devuelve ruido. */
        const val MINIMO_PARA_BUSCAR = 2

        /** Lo que devuelve TMDB en cada pagina. */
        const val POR_PAGINA = 20
        const val ESPERA_ANTES_DE_BUSCAR = 350L
    }
}
