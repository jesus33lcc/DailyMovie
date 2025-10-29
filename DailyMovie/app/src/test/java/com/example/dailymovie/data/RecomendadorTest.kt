package com.example.dailymovie.data

import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.PersonaPopular
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.client.response.ProviderResponse
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba que la portada elige bien la pelicula del dia.
 *
 * Toda la capa de datos contesta por callback, asi que los dobles de aqui contestan en el
 * acto y el test se lee de arriba a abajo: nada de corrutinas, esperas ni red.
 */
class RecomendadorTest {

    // ---- Dobles ----------------------------------------------------------------------

    /**
     * Repositorio de peliculas de mentira: devuelve lo que se le diga al construirlo.
     *
     * Por defecto todas las vias contestan "no tengo nada", asi que cada test solo llena la
     * que le interesa y las demas se caen solas.
     */
    private class PeliculasFalsas(
        private val deGente: Resultado<List<MovieModel>> = Resultado.Exito(emptyList()),
        private val parecidas: Resultado<List<MovieModel>> = Resultado.Exito(emptyList()),
        private val deGeneros: Resultado<List<MovieModel>> = Resultado.Exito(emptyList()),
        private val masVistas: Resultado<List<MovieModel>> = Resultado.Exito(emptyList())
    ) : MovieRepository {

        /** Se guarda lo que se pidio para poder comprobar a quien se le pregunto. */
        var gentePedida: List<Int>? = null
        var referenciaPedida: Int? = null
        var generosPedidos: List<Int>? = null

        override fun deLaGente(personas: List<Int>, alTerminar: (Resultado<List<MovieModel>>) -> Unit) {
            gentePedida = personas
            alTerminar(deGente)
        }

        override fun recomendadas(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) {
            referenciaPedida = peliculaId
            alTerminar(parecidas)
        }

        override fun porGeneros(
            generos: List<Int>,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) {
            generosPedidos = generos
            alTerminar(deGeneros)
        }

        override fun populares(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = alTerminar(masVistas)

        // El resto del repositorio no pinta nada en la recomendacion: si el Recomendador
        // llamara a alguna de estas, el callback no se llamaria y el test lo cantaria.
        override fun buscar(consulta: String, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun buscarTodo(consulta: String, pagina: Int, alTerminar: (Resultado<Pagina>) -> Unit) = Unit
        override fun peliculasDeLaSaga(sagaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun tendencias(alTerminar: (Resultado<List<Hallazgo>>) -> Unit) = Unit
        override fun enCartelera(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun mejorValoradas(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun proximamente(alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun detalles(peliculaId: Int, alTerminar: (Resultado<MovieDetailsResponse>) -> Unit) = Unit
        override fun plataformas(peliculaId: Int, alTerminar: (Resultado<ProviderResponse>) -> Unit) = Unit
        override fun reparto(peliculaId: Int, alTerminar: (Resultado<CreditResponse>) -> Unit) = Unit
        override fun videos(peliculaId: Int, alTerminar: (Resultado<List<VideoModel>>) -> Unit) = Unit
        override fun similares(peliculaId: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) = Unit
        override fun descubrir(
            generos: List<Int>,
            filtros: FiltrosAvanzados,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) = Unit
        override fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit) = Unit
    }

    /** Usuario de mentira: solo hacen falta sus gustos, lo que ha visto y sus favoritas. */
    private class UsuarioFalso(
        private val losGustos: Gustos? = null,
        private val lasVistas: List<MovieModel> = emptyList(),
        private val lasFavoritas: List<MovieModel> = emptyList()
    ) : UserRepository {

        override fun gustos(alTerminar: (Gustos?) -> Unit) = alTerminar(losGustos)
        override fun vistas(alTerminar: (List<MovieModel>) -> Unit) = alTerminar(lasVistas)
        override fun favoritas(alTerminar: (List<MovieModel>) -> Unit) = alTerminar(lasFavoritas)

        override fun haySesion(): Boolean = true
        override fun correoDelUsuario(): String? = "prueba@dailymovie.test"
        override fun registrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun entrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun entrarConGoogle(idToken: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun salir(alTerminar: (Boolean) -> Unit) = Unit
        override fun mandarCorreoDeRecuperacion(correo: String, alTerminar: (Boolean, String?) -> Unit) = Unit
        override fun cambiarContrasena(actual: String, nueva: String, alTerminar: (Boolean, String) -> Unit) = Unit
        override fun borrarCuenta(contrasena: String?, alTerminar: (Boolean, String) -> Unit) = Unit
        override fun esFavorita(peliculaId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun estaVista(peliculaId: Int, alTerminar: (Boolean) -> Unit) = Unit
        override fun cambiarFavorita(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun cambiarVista(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
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
        override fun cambiarSerieFavorita(serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun cambiarSerieVista(serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun seriesDeLista(nombre: String, alTerminar: (List<SerieModel>) -> Unit) = Unit
        override fun anadirSerieALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun quitarSerieDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun listasQueContienenSerie(serieId: Int, alTerminar: (Set<String>) -> Unit) = Unit
        override fun historial(alTerminar: (List<MovieModel>) -> Unit) = Unit
        override fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) = Unit
        override fun borrarHistorial(alTerminar: (Boolean) -> Unit) = Unit
        override fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit) = Unit
        override fun peliculaDelDia(alTerminar: (MovieOfTheDay?) -> Unit) = Unit
    }

    /**
     * Fichas de mentira.
     *
     * Lo que no este en el mapa se contesta como fallo, que es justo el caso de "TMDB no me
     * da el nombre" que la recomendacion tiene que aguantar.
     */
    private class PersonasFalsas(
        private val fichas: Map<Int, PersonModel> = emptyMap()
    ) : PersonRepository {

        var fichaPedida: Int? = null

        override fun ficha(personaId: Int, alTerminar: (Resultado<PersonModel>) -> Unit) {
            fichaPedida = personaId
            val encontrada = fichas[personaId]
            alTerminar(
                if (encontrada != null) Resultado.Exito(encontrada)
                else Resultado.Fallo(ErrorCarga.RESPUESTA_INVALIDA)
            )
        }

        override fun filmografia(personaId: Int, alTerminar: (Resultado<Filmografia>) -> Unit) = Unit
        override fun populares(alTerminar: (Resultado<List<PersonaPopular>>) -> Unit) = Unit
    }

    // ---- Ayudas ----------------------------------------------------------------------

    /** El poster no es adorno: sin el, `elegir` descarta la pelicula. */
    private fun peli(id: Int, titulo: String = "Peli $id", poster: String? = "/poster$id.jpg") =
        MovieModel(id, titulo, "2024-01-01", 7.5, poster)

    private fun persona(id: Int, nombre: String) =
        PersonModel(id, nombre, null, null, null, null, "/foto.jpg", "Acting")

    private fun sinRed(): Resultado<List<MovieModel>> = Resultado.Fallo(ErrorCarga.SIN_CONEXION)

    /** Lanza la recomendacion y devuelve lo que llego, avisando si nadie contesto. */
    private fun recomendar(
        peliculas: MovieRepository,
        personas: PersonRepository,
        usuario: UserRepository,
        semilla: Int
    ): Recomendacion? {
        var contesto = false
        var salida: Recomendacion? = null
        Recomendador(peliculas, personas, usuario).paraHoy(semilla) {
            contesto = true
            salida = it
        }
        // Los dobles contestan en el acto, asi que si aqui no ha contestado es que alguna
        // via se ha dejado el callback colgado y la portada se quedaria en blanco.
        assertTrue("paraHoy no llego a contestar", contesto)
        return salida
    }

    // ---- Tests -----------------------------------------------------------------------

    @Test
    fun `con gente elegida la pelicula sale de esa gente y el motivo la nombra`() {
        val peliculas = PeliculasFalsas(deGente = Resultado.Exito(listOf(peli(10), peli(11))))
        val personas = PersonasFalsas(mapOf(500 to persona(500, "Tom Hardy")))
        val usuario = UsuarioFalso(losGustos = Gustos(personas = listOf(500)))

        val salida = recomendar(peliculas, personas, usuario, semilla = 0)

        assertEquals(10, salida?.pelicula?.id)
        assertEquals("Porque sigues a Tom Hardy", salida?.motivo)
        // Se pregunta por una sola persona: con todas de golpe TMDB devolveria un saco
        // enorme y el motivo dejaria de ser verdad.
        assertEquals(listOf(500), peliculas.gentePedida)
        assertEquals(500, personas.fichaPedida)
    }

    @Test
    fun `si la ficha de la persona falla se recomienda igual con un motivo generico`() {
        val peliculas = PeliculasFalsas(deGente = Resultado.Exito(listOf(peli(10))))
        // Mapa vacio: la ficha contesta fallo, como cuando TMDB no responde.
        val personas = PersonasFalsas()
        val usuario = UsuarioFalso(losGustos = Gustos(personas = listOf(500)))

        val salida = recomendar(peliculas, personas, usuario, semilla = 0)

        assertEquals(10, salida?.pelicula?.id)
        assertEquals("De alguien a quien sigues", salida?.motivo)
    }

    @Test
    fun `si una via no da nada se prueba la siguiente hasta acabar en lo popular`() {
        val peliculas = PeliculasFalsas(
            deGente = Resultado.Exito(emptyList()),   // la gente no da nada
            masVistas = Resultado.Exito(listOf(peli(99)))
        )
        // Sin favoritas, asi que esa via tambien se cae y solo queda lo popular.
        val usuario = UsuarioFalso(losGustos = Gustos(personas = listOf(500)))

        val salida = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 0)

        assertEquals(99, salida?.pelicula?.id)
        assertEquals(Recomendador.MOTIVO_POPULAR, salida?.motivo)
    }

    @Test
    fun `nunca se recomienda algo ya visto ni algo que el usuario ya marco`() {
        val catalogo = Resultado.Exito(listOf(peli(1), peli(2), peli(3)))
        val peliculas = PeliculasFalsas(deGeneros = catalogo, masVistas = catalogo)
        val usuario = UsuarioFalso(
            // La 1 la marco en el onboarding y la 2 la tiene como vista.
            losGustos = Gustos(generos = listOf(28), peliculas = listOf(1)),
            lasVistas = listOf(peli(2))
        )

        // Se prueban varios dias seguidos: cambie la via que cambie, esas dos no salen.
        for (semilla in 0..5) {
            val salida = recomendar(peliculas, PersonasFalsas(), usuario, semilla)
            assertEquals("con la semilla $semilla", 3, salida?.pelicula?.id)
        }
    }

    @Test
    fun `dos dias distintos por la misma via dan peliculas distintas`() {
        val peliculas = PeliculasFalsas(
            deGeneros = Resultado.Exito(listOf(peli(1), peli(2), peli(3)))
        )
        val usuario = UsuarioFalso(losGustos = Gustos(generos = listOf(28)))

        // Las dos semillas son pares, asi que la via de partida es la misma (los generos):
        // lo unico que cambia es la candidata que se coge de la lista.
        val lunes = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 0)
        val miercoles = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 2)

        assertEquals(Recomendador.MOTIVO_GENEROS, lunes?.motivo)
        assertEquals(Recomendador.MOTIVO_GENEROS, miercoles?.motivo)
        assertEquals(1, lunes?.pelicula?.id)
        assertEquals(3, miercoles?.pelicula?.id)
        assertNotEquals(lunes?.pelicula?.id, miercoles?.pelicula?.id)
    }

    @Test
    fun `con favoritas se recomienda algo parecido y nunca la propia favorita`() {
        val peliculas = PeliculasFalsas(
            // La 1 es la propia favorita: TMDB la cuela y hay que descartarla.
            parecidas = Resultado.Exito(listOf(peli(1), peli(20)))
        )
        val usuario = UsuarioFalso(lasFavoritas = listOf(peli(1, "Origen")))

        val salida = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 0)

        assertEquals(20, salida?.pelicula?.id)
        assertEquals("Porque te gustó Origen", salida?.motivo)
        assertEquals(1, peliculas.referenciaPedida)
    }

    @Test
    fun `sin gustos y sin favoritas sale una popular`() {
        val peliculas = PeliculasFalsas(masVistas = Resultado.Exito(listOf(peli(7))))
        // Usuario recien registrado: no ha dicho nada de nada.
        val usuario = UsuarioFalso()

        val salida = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 5)

        assertEquals(7, salida?.pelicula?.id)
        assertEquals(Recomendador.MOTIVO_POPULAR, salida?.motivo)
    }

    @Test
    fun `sin red no se recomienda nada`() {
        val peliculas = PeliculasFalsas(
            deGente = sinRed(),
            parecidas = sinRed(),
            deGeneros = sinRed(),
            masVistas = sinRed()
        )
        val usuario = UsuarioFalso(losGustos = Gustos(generos = listOf(28), personas = listOf(500)))

        val salida = recomendar(peliculas, PersonasFalsas(), usuario, semilla = 0)

        // Devolver null es la respuesta correcta: la vista ya sabe enseñar el error, y una
        // portada inventada seria peor que ninguna.
        assertNull(salida)
    }
}
