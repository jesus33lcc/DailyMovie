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

private const val PUESTOS_PRINCIPALES = 5
private const val EPISODIOS_PARA_SER_FIJO = 4

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

    /**
     * Si merece la pena enseñar este trabajo en la ficha.
     *
     * Se cae lo que no tiene titulo o cartel, que en una rejilla de carteles es un hueco, y
     * lo que nadie ha votado: TMDB guarda muchisimo material de relleno (promocionales,
     * apariciones de un minuto) que no dice nada de la carrera de la persona.
     */
    private fun esEnseñable(trabajo: PeliculaDePersona) =
        trabajo.comoSeLlama.isNotBlank() && !trabajo.poster.isNullOrBlank() && trabajo.votos > 0

    /**
     * Si el papel es de los importantes.
     *
     * En cine se mira el puesto del reparto, que TMDB numera empezando por el protagonista.
     * En television eso no vale, porque cada capitulo tiene su propio reparto: ahi lo que
     * dice si el papel es fijo o una aparicion es en cuantos episodios sale.
     */
    private fun esPapelDePeso(trabajo: PeliculaDePersona) = if (trabajo.esSerie) {
        (trabajo.episodios ?: 0) >= EPISODIOS_PARA_SER_FIJO
    } else {
        (trabajo.puestoEnElReparto ?: 0) <= PUESTOS_PRINCIPALES
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
                                .filter { esEnseñable(it) }
                                // Primero los papeles de peso y dentro de ellos lo mas
                                // conocido: es el orden en el que la gente reconoce a alguien.
                                .sortedWith(
                                    compareBy<PeliculaDePersona> { esPapelDePeso(it).not() }
                                        .thenByDescending { it.popularidad }
                                ),
                            dirigiendo = respuesta.trabajos
                                .filter { esEnseñable(it) && it.puesto == "Director" }
                                .sortedByDescending { it.popularidad }
                        )
                    )
                )
            },
            onError = { alTerminar(Resultado.Fallo(it)) }
        )
    }
}
