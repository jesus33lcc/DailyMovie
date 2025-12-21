package com.example.dailymovie.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.data.AltaDeLista
import com.example.dailymovie.data.Gustos
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Pagina
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.SerieRepository
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.fragments.viewmodels.ExplorarViewModel
import com.example.dailymovie.fragments.viewmodels.FiltroDeBusqueda
import com.example.dailymovie.fragments.viewmodels.OrdenDeBusqueda
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Comprueba el buscador: que lo que llega de TMDB acaba en pantalla, y que los chips de tipo
 * y de orden colocan los resultados como toca sin volver a pedir nada.
 *
 * Aqui hacen falta dos apaños que en [com.example.dailymovie.data.RecomendadorTest] no:
 * el LiveData, que fuera de Android no deja escribir el valor, y la espera del buscador, que
 * de verdad tardaria un tercio de segundo por test. Los dos se resuelven en el @Before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExplorarViewModelTest {

    /** Hace que LiveData publique en el acto, sin el hilo principal de Android. */
    @get:Rule
    val reglaDeLiveData = InstantTaskExecutorRule()

    /**
     * El hilo principal de mentira. Se guarda para poder adelantarle el reloj: la busqueda
     * espera 350 ms antes de salir a la red y el test no va a esperarlos de verdad.
     */
    private val hiloDePrueba = StandardTestDispatcher()

    @Before
    fun ponerHiloPrincipal() {
        Dispatchers.setMain(hiloDePrueba)
    }

    @After
    fun devolverHiloPrincipal() {
        Dispatchers.resetMain()
    }

    /** Salta la espera del buscador y deja que la corrutina llegue hasta el final. */
    private fun adelantarLaEspera() {
        hiloDePrueba.scheduler.advanceUntilIdle()
    }

    // ---- Dobles ----------------------------------------------------------------------

    /**
     * Repositorio de peliculas de mentira: contesta a cada pagina lo que se le haya dicho.
     *
     * Lo que no este en el mapa se contesta como una pagina vacia y sin continuacion, que es
     * como se comporta TMDB cuando te pasas del final.
     */
    private class PeliculasFalsas(
        private val porPagina: Map<Int, Resultado<Pagina>> = emptyMap()
    ) : MovieRepository {

        /** Para comprobar que se pide lo que se debe y no de mas. */
        val paginasPedidas = mutableListOf<Int>()
        var consultaPedida: String? = null

        override fun buscarTodo(consulta: String, pagina: Int, alTerminar: (Resultado<Pagina>) -> Unit) {
            consultaPedida = consulta
            paginasPedidas += pagina
            alTerminar(porPagina[pagina] ?: Resultado.Exito(Pagina(emptyList(), pagina, false)))
        }

        // El buscador no usa el resto: si llamara a alguna, el callback no se llamaria y el
        // test lo cantaria al no ver resultados.
        override fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun peliculasDeLaSaga(sagaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit) = Unit
        override fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit) = Unit
        override fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) = Unit
        override fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) = Unit
        override fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) = Unit
        override fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun resenas(peliculaId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit) = Unit
        override fun deLaGente(personas: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun porGeneros(
            generos: List<Int>,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) = Unit
        override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) = Unit
        override fun descubrir(
            generos: List<Int>,
            filtros: FiltrosAvanzados,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) = Unit
        override fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit) = Unit
    }

    /** Series de mentira: el buscador solo las toca al explorar generos, que aqui no se prueba. */
    private class SeriesFalsas : SerieRepository {
        override fun buscar(consulta: String, alTerminar: (Resultado<List<SerieModel>>) -> Unit) = Unit
        override fun populares(alTerminar: (Resultado<List<SerieModel>>) -> Unit) = Unit
        override fun mejorValoradas(alTerminar: (Resultado<List<SerieModel>>) -> Unit) = Unit
        override fun enEmision(alTerminar: (Resultado<List<SerieModel>>) -> Unit) = Unit
        override fun detalles(serieId: Int, alTerminar: (Resultado<SerieDetailsResponse>) -> Unit) = Unit
        override fun temporada(serieId: Int, numero: Int, alTerminar: (Resultado<SeasonResponse>) -> Unit) = Unit
        override fun reparto(serieId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) = Unit
        override fun videos(serieId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) = Unit
        override fun plataformas(serieId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) = Unit
        override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) = Unit
        override fun porGeneros(
            generos: List<Int>,
            pagina: Int,
            alTerminar: (Resultado<List<SerieModel>>) -> Unit
        ) = Unit

        // Ni la clasificacion ni las reseñas se usan aqui; la interfaz las pide igualmente.
        override fun clasificacionPorEdad(serieId: Int, alTerminar: (String?) -> Unit) =
            alTerminar(null)

        override fun resenas(
            serieId: Int,
            alTerminar: (Resultado<List<ResenaResponse>>) -> Unit
        ) = alTerminar(Resultado.Exito(emptyList()))

        override fun similares(
            serieId: Int,
            alTerminar: (Resultado<List<SerieModel>>) -> Unit
        ) = alTerminar(Resultado.Exito(emptyList()))

        override fun recomendadas(
            serieId: Int,
            alTerminar: (Resultado<List<SerieModel>>) -> Unit
        ) = alTerminar(Resultado.Exito(emptyList()))

        override fun imagenes(serieId: Int, alTerminar: (List<String>) -> Unit) = alTerminar(emptyList())
    }

    /** Usuario de mentira: el buscador solo le pide el historial, que aqui no se prueba. */
    private class UsuarioFalso : UserRepository {
        override fun haySesion(): Boolean = true
        override fun correoDelUsuario(): String? = "prueba@dailymovie.test"
        override fun registrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun entrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun entrarConGoogle(idToken: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun salir(alTerminar: (Boolean) -> Unit) = Unit
        override fun mandarCorreoDeRecuperacion(correo: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun cambiarContrasena(actual: String, nueva: String, alTerminar: (Boolean, String) -> Unit) = Unit
        override fun borrarCuenta(contrasena: String?, alTerminar: (Boolean, String) -> Unit) = Unit
        override fun favoritas(alTerminar: (List<MovieModel>) -> Unit) = Unit
        override fun vistas(alTerminar: (List<MovieModel>) -> Unit) = Unit
        override fun esFavorita(peliculaId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun estaVista(peliculaId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun ponerFavorita(pelicula: MovieModel, favorita: Boolean, alTerminar: (Boolean) -> Unit) = Unit
        override fun ponerVista(pelicula: MovieModel, vista: Boolean, alTerminar: (Boolean) -> Unit) = Unit
        override fun listasDelUsuario(alTerminar: (List<String>) -> Unit) = Unit
        override fun crearLista(nombre: String, alTerminar: (AltaDeLista) -> Unit) = Unit
        override fun borrarLista(nombre: String, alTerminar: (Boolean) -> Unit) = Unit
        override fun peliculasDeLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit) = Unit
        override fun listasQueContienen(peliculaId: Int, alTerminar: (Set<String>) -> Unit) = Unit
        override fun anadirALista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun quitarDeLista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun seriesFavoritas(alTerminar: (List<SerieModel>) -> Unit) = Unit
        override fun seriesVistas(alTerminar: (List<SerieModel>) -> Unit) = Unit
        override fun esSerieFavorita(serieId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun estaSerieVista(serieId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun ponerSerieFavorita(serie: SerieModel, favorita: Boolean, alTerminar: (Boolean) -> Unit) = Unit
        override fun ponerSerieVista(serie: SerieModel, vista: Boolean, alTerminar: (Boolean) -> Unit) = Unit
        override fun seriesDeLista(nombre: String, alTerminar: (List<SerieModel>) -> Unit) = Unit
        override fun anadirSerieALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun quitarSerieDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun listasQueContienenSerie(serieId: Int, alTerminar: (Set<String>) -> Unit) = Unit
        override fun historial(alTerminar: (List<MovieModel>) -> Unit) = Unit
        override fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun borrarHistorial(alTerminar: (Boolean) -> Unit) = Unit
        override fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit) = Unit
        override fun gustos(alTerminar: (Gustos?) -> Unit) = Unit
        override fun peliculaDelDia(alTerminar: (MovieOfTheDay?) -> Unit) = Unit

        // Los episodios vistos no pintan nada aqui; estan porque la interfaz los pide.
        override fun episodiosVistos(serieId: Int, alTerminar: (Set<Pair<Int, Int>>) -> Unit) =
            alTerminar(emptySet())

        override fun ponerEpisodioVisto(
            serieId: Int,
            temporada: Int,
            episodio: Int,
            visto: Boolean,
            alTerminar: (Boolean) -> Unit
        ) = alTerminar(false)

        override fun seriesEmpezadas(alTerminar: (Map<Int, Int>) -> Unit) = alTerminar(emptyMap())
    }

    // ---- Ayudas ----------------------------------------------------------------------

    private fun peli(id: Int, nota: Double = 7.0, ano: String = "2020-01-01") =
        Hallazgo(id, "Peli $id", ano, "/poster$id.jpg", nota, TipoDeHallazgo.PELICULA)

    private fun serie(id: Int, nota: Double = 7.0, ano: String = "2020-01-01") =
        Hallazgo(id, "Serie $id", ano, "/poster$id.jpg", nota, TipoDeHallazgo.SERIE)

    private fun gente(id: Int) =
        Hallazgo(id, "Alguien $id", "Interpretacion", "/foto$id.jpg", 0.0, TipoDeHallazgo.PERSONA)

    private fun pagina(vararg hallazgos: Hallazgo, hayMas: Boolean = false, numero: Int = 1) =
        Resultado.Exito(Pagina(hallazgos.toList(), numero, hayMas))

    /** Monta el ViewModel con el repositorio que le toque y los otros dos de relleno. */
    private fun viewModel(peliculas: MovieRepository) =
        ExplorarViewModel(peliculas, SeriesFalsas(), UsuarioFalso())

    /** Los ids de lo que hay ahora mismo en pantalla, que es lo que se compara casi siempre. */
    private fun idsEnPantalla(vm: ExplorarViewModel) = vm.resultados.value?.map { it.id }

    // ---- Tests -----------------------------------------------------------------------

    @Test
    fun `buscar publica lo que devuelve el repositorio`() {
        val peliculas = PeliculasFalsas(mapOf(1 to pagina(peli(10), serie(20), gente(30))))
        val vm = viewModel(peliculas)

        vm.buscar("interstellar")
        adelantarLaEspera()

        assertEquals(listOf(10, 20, 30), idsEnPantalla(vm))
        assertEquals("interstellar", peliculas.consultaPedida)
        // Una sola peticion aunque el usuario haya escrito una palabra larga: la espera de
        // antes de buscar esta justo para eso.
        assertEquals(listOf(1), peliculas.paginasPedidas)
        assertEquals(false, vm.sinResultados.value)
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `escribir deprisa solo lanza la ultima busqueda`() {
        val peliculas = PeliculasFalsas(mapOf(1 to pagina(peli(10))))
        val vm = viewModel(peliculas)

        // Tres teclas seguidas sin que de tiempo a que salga ninguna peticion.
        vm.buscar("int")
        vm.buscar("inte")
        vm.buscar("inter")
        adelantarLaEspera()

        assertEquals(listOf(1), peliculas.paginasPedidas)
        assertEquals("inter", peliculas.consultaPedida)
    }

    @Test
    fun `si la busqueda no encuentra nada se dice, y si falla se avisa del motivo`() {
        val vaciaVm = viewModel(PeliculasFalsas(mapOf(1 to pagina())))
        vaciaVm.buscar("asdfgh")
        adelantarLaEspera()

        assertEquals(emptyList<Int>(), idsEnPantalla(vaciaVm))
        assertEquals(true, vaciaVm.sinResultados.value)

        val sinRedVm = viewModel(
            PeliculasFalsas(mapOf(1 to Resultado.Fallo(ErrorCarga.SIN_CONEXION)))
        )
        sinRedVm.buscar("asdfgh")
        adelantarLaEspera()

        // Sin red no es "no hay resultados": es un error, y la vista pone otro texto.
        assertEquals(false, sinRedVm.sinResultados.value)
        assertEquals(ErrorCarga.SIN_CONEXION, sinRedVm.error.value)
    }

    @Test
    fun `el filtro por tipo deja pasar solo lo de ese tipo y los chips cuentan bien`() {
        val vm = viewModel(
            PeliculasFalsas(mapOf(1 to pagina(peli(10), serie(20), peli(11), gente(30))))
        )
        vm.buscar("nolan")
        adelantarLaEspera()

        vm.cambiarFiltro(FiltroDeBusqueda(tipo = TipoDeHallazgo.PELICULA))
        assertEquals(listOf(10, 11), idsEnPantalla(vm))

        vm.cambiarFiltro(FiltroDeBusqueda(tipo = TipoDeHallazgo.SERIE))
        assertEquals(listOf(20), idsEnPantalla(vm))

        vm.cambiarFiltro(FiltroDeBusqueda(tipo = TipoDeHallazgo.PERSONA))
        assertEquals(listOf(30), idsEnPantalla(vm))

        // Sin tipo vuelve todo, en el orden en que lo mando TMDB.
        vm.cambiarFiltro(FiltroDeBusqueda())
        assertEquals(listOf(10, 20, 11, 30), idsEnPantalla(vm))

        // Los numeros de los chips cuentan sobre lo descargado, no sobre lo que se ve: si
        // contaran lo que se ve, al filtrar por series el chip de peliculas diria cero.
        assertEquals(4, vm.cuantosHayDe(null))
        assertEquals(2, vm.cuantosHayDe(TipoDeHallazgo.PELICULA))
        assertEquals(1, vm.cuantosHayDe(TipoDeHallazgo.SERIE))
        assertEquals(1, vm.cuantosHayDe(TipoDeHallazgo.PERSONA))
        assertEquals(0, vm.cuantosHayDe(TipoDeHallazgo.SAGA))
    }

    @Test
    fun `cambiar de chip no vuelve a pedir nada a la red`() {
        val peliculas = PeliculasFalsas(mapOf(1 to pagina(peli(10), serie(20))))
        val vm = viewModel(peliculas)
        vm.buscar("dune")
        adelantarLaEspera()

        vm.cambiarFiltro(FiltroDeBusqueda(tipo = TipoDeHallazgo.SERIE))
        vm.cambiarFiltro(FiltroDeBusqueda(orden = OrdenDeBusqueda.NOTA))

        // Los filtros se aplican encima de lo ya descargado: una sola peticion en todo el test.
        assertEquals(listOf(1), peliculas.paginasPedidas)
    }

    @Test
    fun `ordenar por nota y por año coloca los resultados como toca`() {
        val vm = viewModel(
            PeliculasFalsas(
                mapOf(
                    1 to pagina(
                        peli(10, nota = 6.1, ano = "2015-05-01"),
                        peli(11, nota = 8.9, ano = "1999-03-20"),
                        peli(12, nota = 7.4, ano = "2023-11-02")
                    )
                )
            )
        )
        vm.buscar("cualquiera")
        adelantarLaEspera()

        // Relevancia es no tocar nada: TMDB ya manda lo que mas encaja primero.
        vm.cambiarFiltro(FiltroDeBusqueda(orden = OrdenDeBusqueda.RELEVANCIA))
        assertEquals(listOf(10, 11, 12), idsEnPantalla(vm))

        vm.cambiarFiltro(FiltroDeBusqueda(orden = OrdenDeBusqueda.NOTA))
        assertEquals(listOf(11, 12, 10), idsEnPantalla(vm))

        vm.cambiarFiltro(FiltroDeBusqueda(orden = OrdenDeBusqueda.ANO))
        assertEquals(listOf(12, 10, 11), idsEnPantalla(vm))

        // Tipo y orden a la vez: primero se quita lo que no es del tipo, luego se ordena.
        vm.cambiarFiltro(FiltroDeBusqueda(tipo = TipoDeHallazgo.PELICULA, orden = OrdenDeBusqueda.NOTA))
        assertEquals(listOf(11, 12, 10), idsEnPantalla(vm))
    }

    @Test
    fun `cargar mas añade la pagina siguiente sin repetir lo que ya estaba`() {
        val peliculas = PeliculasFalsas(
            mapOf(
                1 to pagina(peli(10), peli(11), hayMas = true),
                // TMDB devuelve la 11 otra vez entre paginas; con el mismo id la lista se queja.
                2 to pagina(peli(11), peli(12), numero = 2, hayMas = false)
            )
        )
        val vm = viewModel(peliculas)
        vm.buscar("saga")
        adelantarLaEspera()
        assertEquals(listOf(10, 11), idsEnPantalla(vm))

        vm.cargarMas()

        assertEquals(listOf(10, 11, 12), idsEnPantalla(vm))
        assertEquals(listOf(1, 2), peliculas.paginasPedidas)
        assertEquals(3, vm.cuantosHayDe(null))
    }

    @Test
    fun `cargar mas no pide nada cuando ya no quedan paginas`() {
        val peliculas = PeliculasFalsas(mapOf(1 to pagina(peli(10), hayMas = false)))
        val vm = viewModel(peliculas)
        vm.buscar("algo")
        adelantarLaEspera()

        // El scroll dispara esto muchas veces seguidas; si no se frenara, se pedirian varias
        // paginas iguales a la vez.
        vm.cargarMas()
        vm.cargarMas()

        assertEquals(listOf(1), peliculas.paginasPedidas)
        assertEquals(listOf(10), idsEnPantalla(vm))
    }

    @Test
    fun `borrar la busqueda deja la lista vacia`() {
        val peliculas = PeliculasFalsas(mapOf(1 to pagina(peli(10), peli(11), hayMas = true)))
        val vm = viewModel(peliculas)
        vm.buscar("interstellar")
        adelantarLaEspera()
        assertTrue(vm.hayBusquedaEnMarcha())

        vm.buscar("")
        adelantarLaEspera()

        assertEquals(emptyList<Int>(), idsEnPantalla(vm))
        assertEquals(0, vm.cuantosHayDe(null))
        assertFalse(vm.hayBusquedaEnMarcha())
        assertEquals(false, vm.sinResultados.value)
        // Con el campo vacio no se sale a la red: sigue habiendo una sola peticion, la de antes.
        assertEquals(listOf(1), peliculas.paginasPedidas)
    }
}
