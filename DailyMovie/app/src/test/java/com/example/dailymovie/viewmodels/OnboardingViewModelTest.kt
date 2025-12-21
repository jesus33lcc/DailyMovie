package com.example.dailymovie.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailymovie.activities.viewmodels.OnboardingViewModel
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.PersonaPopular
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.data.AltaDeLista
import com.example.dailymovie.data.Filmografia
import com.example.dailymovie.data.Gustos
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Pagina
import com.example.dailymovie.data.PersonRepository
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.CastMemberModel
import com.example.dailymovie.models.CrewMemberModel
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.models.PersonModel
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Comprueba las tres preguntas del onboarding: que lo que marca el usuario se queda marcado,
 * que lo que se le ofrece depende de lo que ya ha elegido, y que al final se guarda eso mismo
 * y no otra cosa.
 *
 * Los dobles contestan en el acto, asi que los tests se leen de arriba a abajo. Lo unico que
 * hace falta de Android es la regla del LiveData, que fuera del movil no deja publicar valores.
 */
class OnboardingViewModelTest {

    /** Hace que LiveData publique en el acto, sin el hilo principal de Android. */
    @get:Rule
    val reglaDeLiveData = InstantTaskExecutorRule()

    // ---- Dobles ----------------------------------------------------------------------

    /**
     * Repositorio de peliculas de mentira.
     *
     * Apunta a quien se le ha preguntado, que en el onboarding importa tanto como la respuesta:
     * la gracia esta en que la segunda pregunta salga de lo elegido en la primera.
     */
    private class PeliculasFalsas(
        private val populares: List<MovieModel> = emptyList(),
        private val deGeneros: List<MovieModel> = emptyList(),
        private val repartos: Map<Int, CreditResponse> = emptyMap()
    ) : MovieRepository {

        var generosPedidos: List<Int>? = null
        var pidioPopulares = false
        val repartosPedidos = mutableListOf<Int>()

        override fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit) {
            pidioPopulares = true
            alTerminar(Resultado.Exito(populares))
        }

        override fun porGeneros(
            generos: List<Int>,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) {
            generosPedidos = generos
            alTerminar(Resultado.Exito(deGeneros))
        }

        override fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) {
            repartosPedidos += peliculaId
            val encontrado = repartos[peliculaId]
            alTerminar(
                if (encontrado != null) Resultado.Exito(encontrado)
                else Resultado.Fallo(ErrorCarga.RESPUESTA_INVALIDA)
            )
        }

        override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) =
            alTerminar(Resultado.Exito(listOf(GenreModel(28, "Accion"), GenreModel(35, "Comedia"))))

        // El onboarding no usa el resto: si llamara a alguna, el callback no se llamaria y el
        // test lo cantaria al no ver nada publicado.
        override fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun buscarTodo(consulta: String, pagina: Int, alTerminar: (Resultado<Pagina>) -> Unit) = Unit
        override fun peliculasDeLaSaga(sagaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit) = Unit
        override fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit) = Unit
        override fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) = Unit
        override fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) = Unit
        override fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun resenas(peliculaId: Int, alTerminar: (Resultado<List<ResenaResponse>>) -> Unit) = Unit
        override fun deLaGente(personas: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun descubrir(
            generos: List<Int>,
            filtros: FiltrosAvanzados,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) = Unit
        override fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit) = Unit
    }

    /** Caras conocidas de mentira, para el caso de que el usuario no marque ninguna pelicula. */
    private class PersonasFalsas(
        private val lasPopulares: List<PersonaPopular> = emptyList()
    ) : PersonRepository {

        var pidioPopulares = false

        override fun populares(alTerminar: (Resultado<List<PersonaPopular>>) -> Unit) {
            pidioPopulares = true
            alTerminar(Resultado.Exito(lasPopulares))
        }

        override fun ficha(personaId: Int, alTerminar: (Resultado<PersonModel>) -> Unit) = Unit
        override fun filmografia(personaId: Int, alTerminar: (Resultado<Filmografia>) -> Unit) = Unit

        // Las fotos no se usan aqui; la interfaz las pide igualmente.
        override fun fotos(personaId: Int, alTerminar: (List<String>) -> Unit) = alTerminar(emptyList())
    }

    /**
     * Usuario de mentira: se queda con lo ultimo que se le mando guardar.
     *
     * Es la unica pieza que importa aqui, porque el onboarding no sirve de nada si lo que
     * acaba en Firestore no es lo que el usuario ha ido tocando.
     */
    private class UsuarioFalso(
        private val losGustos: Gustos? = null,
        private val guardaBien: Boolean = true
    ) : UserRepository {

        var gustosGuardados: Gustos? = null

        override fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit) {
            gustosGuardados = gustos
            alTerminar(guardaBien)
        }

        override fun gustos(alTerminar: (Gustos?) -> Unit) = alTerminar(losGustos)

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

    private fun peli(id: Int) = MovieModel(id, "Peli $id", "2024-01-01", 7.5, "/poster$id.jpg")

    /** Alguien del reparto. Sin foto no vale: la pantalla es una rejilla de caras. */
    private fun actor(id: Int, nombre: String, foto: String? = "/foto$id.jpg") = CastMemberModel(
        adult = false, gender = 2, id = id, knownForDepartment = "Acting", name = nombre,
        originalName = nombre, popularity = 10.0, profilePath = foto, castId = id,
        character = "Alguien", creditId = "c$id", order = 0
    )

    /** Alguien del equipo. Solo interesan los directores; el resto se queda fuera. */
    private fun equipo(id: Int, nombre: String, puesto: String, foto: String? = "/foto$id.jpg") =
        CrewMemberModel(
            adult = false, gender = 2, id = id, knownForDepartment = "Directing", name = nombre,
            originalName = nombre, popularity = 10.0, profilePath = foto, creditId = "e$id",
            department = "Directing", job = puesto
        )

    private fun creditos(id: Int, reparto: List<CastMemberModel>, equipo: List<CrewMemberModel>) =
        CreditResponse(id, reparto, equipo)

    private fun viewModel(
        peliculas: MovieRepository = PeliculasFalsas(),
        usuario: UserRepository = UsuarioFalso(),
        personas: PersonRepository = PersonasFalsas()
    ) = OnboardingViewModel(peliculas, usuario, personas)

    // ---- Tests: marcar y desmarcar ---------------------------------------------------

    @Test
    fun `marcar y desmarcar generos se refleja en lo elegido`() {
        val vm = viewModel()
        assertFalse(vm.hayAlgunGenero())

        vm.marcarGenero(28, true)
        vm.marcarGenero(35, true)

        assertTrue(vm.generoEstaElegido(28))
        assertTrue(vm.generoEstaElegido(35))
        assertFalse(vm.generoEstaElegido(12))
        assertTrue(vm.hayAlgunGenero())

        vm.marcarGenero(28, false)

        assertFalse(vm.generoEstaElegido(28))
        assertTrue(vm.generoEstaElegido(35))
        // Queda uno, asi que sigue habiendo genero: la pantalla no debe dejar de avanzar.
        assertTrue(vm.hayAlgunGenero())

        vm.marcarGenero(35, false)
        assertFalse(vm.hayAlgunGenero())
    }

    @Test
    fun `marcar y desmarcar peliculas y personas se refleja en lo elegido`() {
        val vm = viewModel()

        vm.marcarPelicula(500, true)
        vm.marcarPersona(900, true)

        assertTrue(vm.peliculaEstaElegida(500))
        assertTrue(vm.personaEstaElegida(900))
        // Los tres conjuntos van por separado: un id de pelicula no marca a una persona.
        assertFalse(vm.personaEstaElegida(500))
        assertFalse(vm.peliculaEstaElegida(900))

        vm.marcarPelicula(500, false)
        vm.marcarPersona(900, false)

        assertFalse(vm.peliculaEstaElegida(500))
        assertFalse(vm.personaEstaElegida(900))
    }

    @Test
    fun `marcar dos veces lo mismo no lo duplica al guardar`() {
        val usuario = UsuarioFalso()
        val vm = viewModel(usuario = usuario)

        vm.marcarGenero(28, true)
        vm.marcarGenero(28, true)
        vm.guardar()

        assertEquals(listOf(28), usuario.gustosGuardados?.generos)
    }

    // ---- Tests: lo que se ofrece en cada paso ----------------------------------------

    @Test
    fun `los generos que se ofrecen son los que trae el repositorio`() {
        val vm = viewModel(PeliculasFalsas())

        vm.cargarGeneros()

        assertEquals(listOf(28, 35), vm.generos.value?.map { it.id })
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `con generos elegidos las peliculas sugeridas salen de esos generos`() {
        val peliculas = PeliculasFalsas(
            populares = listOf(peli(99)),
            deGeneros = listOf(peli(10), peli(11))
        )
        val vm = viewModel(peliculas)

        vm.marcarGenero(28, true)
        vm.marcarGenero(12, true)
        vm.cargarPeliculasSugeridas()

        assertEquals(listOf(10, 11), vm.peliculasSugeridas.value?.map { it.id })
        assertEquals(listOf(28, 12), peliculas.generosPedidos)
        // Lo popular no se toca: la gracia de la segunda pregunta es que ya vaya con lo suyo.
        assertFalse(peliculas.pidioPopulares)
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `sin ningun genero elegido las peliculas sugeridas salen de lo popular`() {
        val peliculas = PeliculasFalsas(
            populares = listOf(peli(99)),
            deGeneros = listOf(peli(10))
        )
        val vm = viewModel(peliculas)

        vm.cargarPeliculasSugeridas()

        assertEquals(listOf(99), vm.peliculasSugeridas.value?.map { it.id })
        assertTrue(peliculas.pidioPopulares)
        assertNull(peliculas.generosPedidos)
    }

    @Test
    fun `las personas sugeridas salen del reparto de lo marcado y sin repetir a nadie`() {
        val peliculas = PeliculasFalsas(
            repartos = mapOf(
                1 to creditos(
                    1,
                    reparto = listOf(
                        actor(100, "Cillian Murphy"),
                        // Sin foto: en una rejilla de caras esto seria un hueco.
                        actor(101, "Sin Foto", foto = null)
                    ),
                    equipo = listOf(
                        equipo(200, "Christopher Nolan", "Director"),
                        // No es director: el onboarding no ofrece productores ni guionistas.
                        equipo(201, "Emma Thomas", "Producer")
                    )
                ),
                2 to creditos(
                    2,
                    // Cillian sale en las dos; tiene que aparecer una sola vez.
                    reparto = listOf(actor(100, "Cillian Murphy"), actor(102, "Florence Pugh")),
                    equipo = emptyList()
                )
            )
        )
        val personas = PersonasFalsas(listOf(PersonaPopular(999, "Famoso", "/f.jpg", "Acting")))
        val vm = viewModel(peliculas, personas = personas)

        vm.marcarPelicula(1, true)
        vm.marcarPelicula(2, true)
        vm.cargarPersonasSugeridas()

        assertEquals(listOf(100, 200, 102), vm.personasSugeridas.value?.map { it.id })
        assertEquals(listOf("Cillian Murphy", "Christopher Nolan", "Florence Pugh"),
            vm.personasSugeridas.value?.map { it.nombre })
        // A los directores se les marca como tal aunque TMDB no lo diga en ese campo.
        assertEquals("Directing", vm.personasSugeridas.value?.first { it.id == 200 }?.oficio)
        assertFalse(personas.pidioPopulares)
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `sin peliculas marcadas las personas sugeridas salen de las populares`() {
        val personas = PersonasFalsas(
            listOf(
                PersonaPopular(900, "Actriz", "/a.jpg", "Acting"),
                PersonaPopular(901, "Actor", "/b.jpg", "Acting")
            )
        )
        val peliculas = PeliculasFalsas()
        val vm = viewModel(peliculas, personas = personas)

        vm.cargarPersonasSugeridas()

        assertEquals(listOf(900, 901), vm.personasSugeridas.value?.map { it.id })
        assertTrue(personas.pidioPopulares)
        // Ni un solo reparto pedido: no hay de donde sacarlo.
        assertEquals(emptyList<Int>(), peliculas.repartosPedidos)
    }

    @Test
    fun `aunque se marquen muchas peliculas solo se piden cuatro repartos`() {
        val peliculas = PeliculasFalsas(
            repartos = (1..6).associateWith { creditos(it, listOf(actor(it * 10, "Actor $it")), emptyList()) }
        )
        val vm = viewModel(peliculas)

        (1..6).forEach { vm.marcarPelicula(it, true) }
        vm.cargarPersonasSugeridas()

        // Con seis peticiones la pantalla tardaria demasiado y las caras se repetirian igual.
        assertEquals(listOf(1, 2, 3, 4), peliculas.repartosPedidos)
        assertEquals(listOf(10, 20, 30, 40), vm.personasSugeridas.value?.map { it.id })
    }

    // ---- Tests: guardar --------------------------------------------------------------

    @Test
    fun `guardar manda al repositorio exactamente lo que el usuario eligio`() {
        val usuario = UsuarioFalso()
        val vm = viewModel(usuario = usuario)

        vm.marcarGenero(28, true)
        vm.marcarGenero(35, true)
        // Marcada y luego desmarcada: no puede acabar en Firestore.
        vm.marcarGenero(12, true)
        vm.marcarGenero(12, false)
        vm.marcarPelicula(500, true)
        vm.marcarPersona(900, true)
        vm.marcarPersona(901, true)

        vm.guardar()

        assertEquals(Gustos(listOf(28, 35), listOf(500), listOf(900, 901)), usuario.gustosGuardados)
        assertEquals(true, vm.guardado.value)
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `se puede guardar sin haber elegido nada`() {
        val usuario = UsuarioFalso()
        val vm = viewModel(usuario = usuario)

        vm.guardar()

        // La portada tirara de lo popular y el usuario podra volver a esto desde Ajustes.
        assertEquals(Gustos(), usuario.gustosGuardados)
        assertEquals(true, vm.guardado.value)
    }

    @Test
    fun `si el guardado falla se dice, para poder avisar en la pantalla`() {
        val usuario = UsuarioFalso(guardaBien = false)
        val vm = viewModel(usuario = usuario)

        vm.guardar()

        assertEquals(false, vm.guardado.value)
        assertEquals(false, vm.cargando.value)
    }

    @Test
    fun `volver desde ajustes precarga lo que ya estaba guardado`() {
        val usuario = UsuarioFalso(losGustos = Gustos(listOf(28), listOf(500), listOf(900)))
        val vm = viewModel(usuario = usuario)

        var termino = false
        vm.cargarGustosGuardados { termino = true }

        assertTrue("cargarGustosGuardados no llego a avisar", termino)
        assertTrue(vm.generoEstaElegido(28))
        assertTrue(vm.peliculaEstaElegida(500))
        assertTrue(vm.personaEstaElegida(900))

        // Y al volver a guardar sale lo mismo, mas lo que toque el usuario ahora.
        vm.marcarGenero(35, true)
        vm.guardar()

        assertEquals(listOf(28, 35), usuario.gustosGuardados?.generos)
    }
}
