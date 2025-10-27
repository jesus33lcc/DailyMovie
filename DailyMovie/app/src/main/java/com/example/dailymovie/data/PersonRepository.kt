package com.example.dailymovie.data

import com.example.dailymovie.client.RetrofitClient
import com.example.dailymovie.client.WebService
import com.example.dailymovie.client.enqueueSimple
import com.example.dailymovie.client.response.PeliculaDePersona
import com.example.dailymovie.client.response.PersonaPopular
import com.example.dailymovie.models.PersonModel
import com.example.dailymovie.utils.Constantes

/** Fichas de actores y directores, con su filmografia. */
interface PersonRepository {
    fun ficha(personaId: Int, alTerminar: (Resultado<PersonModel>) -> Unit)
    fun filmografia(personaId: Int, alTerminar: (Resultado<Filmografia>) -> Unit)

    /**
     * Caras conocidas para elegir en el onboarding.
     *
     * @param alTerminar recibe la lista ya limpia de gente sin foto, que en una rejilla de
     *   caras es justo lo que no se puede enseñar.
     */
    fun populares(alTerminar: (Resultado<List<PersonaPopular>>) -> Unit)
}

/**
 * Lo que ha hecho una persona, ya separado en lo que interesa enseñar.
 *
 * TMDB devuelve "cast" y "crew" en bruto; aqui se traduce a como lo ve el usuario: en lo
 * que ha actuado y lo que ha dirigido.
 */
data class Filmografia(
    val actuando: List<PeliculaDePersona>,
    val dirigiendo: List<PeliculaDePersona>
) {
    fun estaVacia() = actuando.isEmpty() && dirigiendo.isEmpty()
}

class TmdbPersonRepository(
    private val servicio: WebService = RetrofitClient.webService,
    private val apiKey: String = Constantes.API_KEY
) : PersonRepository {

    override fun ficha(personaId: Int, alTerminar: (Resultado<PersonModel>) -> Unit) {
        servicio.getPersona(personaId, apiKey).enqueueSimple(
            onExito = { alTerminar(Resultado.Exito(it)) },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun populares(alTerminar: (Resultado<List<PersonaPopular>>) -> Unit) {
        servicio.getPersonasPopulares(apiKey).enqueueSimple(
            onExito = { respuesta ->
                alTerminar(Resultado.Exito(respuesta.resultados.filter { !it.foto.isNullOrBlank() }))
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }

    override fun filmografia(personaId: Int, alTerminar: (Resultado<Filmografia>) -> Unit) {
        servicio.getFilmografia(personaId, apiKey).enqueueSimple(
            onExito = { respuesta ->
                alTerminar(
                    Resultado.Exito(
                        Filmografia(
                            // Las mas valoradas primero: es lo que la gente busca de un actor.
                            actuando = respuesta.actuaciones
                                .filter { it.titulo != null }
                                .sortedByDescending { it.valoracion },
                            dirigiendo = respuesta.trabajos
                                .filter { it.titulo != null && it.puesto == "Director" }
                                .sortedByDescending { it.valoracion }
                        )
                    )
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }
}
