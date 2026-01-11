package com.example.dailymovie.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dailymovie.activities.viewmodels.SagaViewModel
import com.example.dailymovie.client.response.CreditResponse
import com.example.dailymovie.client.response.MovieDetailsResponse
import com.example.dailymovie.client.response.PlataformaDisponible
import com.example.dailymovie.client.response.ProviderResponse
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.data.MovieRepository
import com.example.dailymovie.data.Pagina
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.models.GenreModel
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.ErrorCarga
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * La pantalla de una saga.
 *
 * Antes esto no se podía probar: `SagaA` hablaba con el repositorio desde `onCreate`, así que
 * para comprobar el orden de las películas hacía falta un dispositivo. Con el ViewModel se
 * prueba con un doble y sin salir a la red.
 */
class SagaViewModelTest {

    @get:Rule
    val reglaDeHiloPrincipal = InstantTaskExecutorRule()

    @Test
    fun `las peliculas de la saga salen en orden de estreno`() {
        // TMDB no las devuelve ordenadas, y una saga desordenada no sirve de nada.
        val peliculas = PeliculasDeSagaFalsas(
            Resultado.Exito(
                listOf(peli(3, "1990-12-25"), peli(1, "1972-03-14"), peli(2, "1974-12-20"))
            )
        )
        val vm = SagaViewModel(peliculas)

        vm.cargar(230)

        assertEquals(listOf(1, 2, 3), vm.peliculasDeLaSaga.value?.map { it.id })
    }

    @Test
    fun `girar el movil no vuelve a pedirlas`() {
        val peliculas = PeliculasDeSagaFalsas(Resultado.Exito(listOf(peli(1, "1972-03-14"))))
        val vm = SagaViewModel(peliculas)

        vm.cargar(230)
        vm.cargar(230)

        assertEquals(1, peliculas.vecesPedida)
    }

    @Test
    fun `si falla se avisa y se apaga el indicador`() {
        val vm = SagaViewModel(PeliculasDeSagaFalsas(Resultado.Fallo(ErrorCarga.SIN_CONEXION)))

        vm.cargar(230)

        assertEquals(ErrorCarga.SIN_CONEXION, vm.error.value)
        assertEquals(false, vm.cargando.value)
    }

    private fun peli(id: Int, estreno: String) =
        MovieModel(id, "Peli $id", estreno, 8.0, "/poster$id.jpg")

    /** Solo contesta a lo de la saga; lo demás no lo usa esta pantalla. */
    private class PeliculasDeSagaFalsas(
        private val respuesta: Resultado<List<MovieModel>>
    ) : MovieRepository {

        var vecesPedida = 0

        override fun peliculasDeLaSaga(
            sagaId: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) {
            vecesPedida++
            alTerminar(respuesta)
        }

        override fun buscarTodo(consulta: String, pagina: Int, alTerminar: (Resultado<Pagina>) -> Unit) = Unit
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
        override fun porGeneros(generos: List<Int>, pagina: Int, alTerminar: (Resultado<List<MovieModel>>) -> Unit) = Unit
        override fun descubrir(
            generos: List<Int>,
            filtros: FiltrosAvanzados,
            pagina: Int,
            alTerminar: (Resultado<List<MovieModel>>) -> Unit
        ) = Unit
        override fun plataformasDisponibles(alTerminar: (Resultado<List<PlataformaDisponible>>) -> Unit) = Unit
        override fun generos(alTerminar: (Resultado<List<GenreModel>>) -> Unit) = Unit
    }
}
