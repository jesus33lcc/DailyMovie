package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel

/**
 * Una propuesta para la portada: la película y por qué se le enseña.
 *
 * El motivo no es adorno. Una recomendación sin explicación parece un anuncio; con ella el
 * usuario entiende que la app le ha entendido, y si no le encaja sabe qué cambiar en sus
 * gustos.
 */
data class Recomendacion(val pelicula: MovieModel, val motivo: String)

/**
 * Elige la película que se enseña al abrir la app.
 *
 * Antes esto era una línea: coger la primera de los géneros elegidos. El problema es que la
 * primera de "acción + drama" es la misma hoy, mañana y dentro de un mes, así que la portada
 * se quedaba congelada y los gustos que el usuario había ido dando (favoritos, actores) no
 * servían para nada.
 *
 * Ahora hay cuatro vías y cada día se prueba una distinta, de más personal a menos:
 *
 *  1. **La gente que sigues** — algo de un actor o un director que marcó en el onboarding.
 *  2. **Lo que te gustó** — parecidas a una de sus favoritas.
 *  3. **Tus géneros** — lo de siempre, cuando no hay nada mejor.
 *  4. **Lo popular** — para el usuario recién registrado que no ha dicho nada.
 *
 * Si una vía no da nada se cae a la siguiente, así que la portada nunca se queda vacía.
 *
 * @param peliculas de dónde salen las películas.
 * @param personas para poder decir el nombre del actor o director en el motivo.
 * @param usuario de dónde salen los gustos y las listas.
 */
class Recomendador(
    private val peliculas: MovieRepository = Dependencias.peliculas,
    private val personas: PersonRepository = Dependencias.personas,
    private val usuario: UserRepository = Dependencias.usuario
) {

    /**
     * Busca la película de hoy.
     *
     * @param semilla número que decide por qué vía se empieza y qué candidata se coge. Se le
     *   pasa el día del año para que la portada cambie cada día pero no cada vez que se
     *   entra: si cambiara a cada rato no habría "película del día" que valga.
     * @param alTerminar recibe la propuesta, o null si ni siquiera hay populares (sin red).
     */
    fun paraHoy(semilla: Int, alTerminar: (Recomendacion?) -> Unit) {
        usuario.gustos { gustos ->
            usuario.vistas { vistas ->
                val descartadas = vistas.map { it.id }.toSet() + gustos?.peliculas.orEmpty()
                val guion = ordenDeVias(semilla, gustos)
                intentar(guion, 0, semilla, gustos, descartadas, alTerminar)
            }
        }
    }

    /**
     * Qué vías se pueden probar hoy y en qué orden.
     *
     * Solo entran las que tienen datos: no sirve de nada intentar "la gente que sigues" si
     * el usuario no marcó a nadie. Entre las que quedan se rota con la semilla, para que dos
     * días seguidos no salga lo mismo.
     */
    private fun ordenDeVias(semilla: Int, gustos: Gustos?): List<Via> {
        val personales = mutableListOf<Via>()
        if (!gustos?.personas.isNullOrEmpty()) personales += Via.GENTE
        if (!gustos?.generos.isNullOrEmpty()) personales += Via.GENEROS
        personales += Via.FAVORITAS

        // Se rota, no se baraja: así todas las vías personales acaban saliendo y ninguna se
        // come siempre el turno.
        val desde = if (personales.isEmpty()) 0 else semilla % personales.size
        // Lo popular va detrás de todo pase lo que pase: es la red de seguridad, y si se
        // colara en medio cortaría el paso a las vías personales de más abajo.
        return personales.drop(desde) + personales.take(desde) + Via.POPULARES
    }

    private fun intentar(
        guion: List<Via>,
        cual: Int,
        semilla: Int,
        gustos: Gustos?,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        if (cual >= guion.size) {
            alTerminar(null)
            return
        }
        val siguiente = { intentar(guion, cual + 1, semilla, gustos, descartadas, alTerminar) }

        when (guion[cual]) {
            Via.GENTE -> porLaGente(semilla, gustos, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.FAVORITAS -> porFavoritas(semilla, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.GENEROS -> porGeneros(semilla, gustos, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.POPULARES -> porPopulares(descartadas) { it?.let(alTerminar) ?: siguiente() }
        }
    }

    private fun porLaGente(
        semilla: Int,
        gustos: Gustos?,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        val elegidas = gustos?.personas.orEmpty()
        if (elegidas.isEmpty()) {
            alTerminar(null)
            return
        }
        // Una sola persona por día: si se pasaran todas, TMDB devolvería lo popular de un
        // saco enorme y el motivo dejaría de ser verdad.
        val quien = elegidas[semilla % elegidas.size]
        peliculas.deLaGente(listOf(quien)) { resultado ->
            val candidata = elegir(resultado, semilla, descartadas)
            if (candidata == null) {
                alTerminar(null)
                return@deLaGente
            }
            // El nombre hace falta para el motivo, pero si falla no se tira la
            // recomendación: se dice en genérico y ya.
            personas.ficha(quien) { ficha ->
                val nombre = (ficha as? Resultado.Exito)?.datos?.nombre
                alTerminar(
                    Recomendacion(
                        candidata,
                        if (nombre.isNullOrBlank()) "De alguien a quien sigues"
                        else "Porque sigues a $nombre"
                    )
                )
            }
        }
    }

    private fun porFavoritas(
        semilla: Int,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        usuario.favoritas { favoritas ->
            if (favoritas.isEmpty()) {
                alTerminar(null)
                return@favoritas
            }
            val referencia = favoritas[semilla % favoritas.size]
            peliculas.recomendadas(referencia.id) { resultado ->
                // La propia referencia también se descarta: recomendar la favorita que ya
                // tienes guardada sería el colmo.
                val candidata = elegir(resultado, semilla, descartadas + referencia.id)
                alTerminar(
                    candidata?.let { Recomendacion(it, "Porque te gustó ${referencia.title}") }
                )
            }
        }
    }

    private fun porGeneros(
        semilla: Int,
        gustos: Gustos?,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        val generos = gustos?.generos.orEmpty()
        if (generos.isEmpty()) {
            alTerminar(null)
            return
        }
        peliculas.porGeneros(generos) { resultado ->
            val candidata = elegir(resultado, semilla, descartadas)
            alTerminar(candidata?.let { Recomendacion(it, MOTIVO_GENEROS) })
        }
    }

    private fun porPopulares(descartadas: Set<Int>, alTerminar: (Recomendacion?) -> Unit) {
        peliculas.populares { resultado ->
            // Aquí no se rota: si se ha llegado hasta abajo es que no sabemos nada del
            // usuario, y lo que toca es enseñarle lo que ve todo el mundo.
            val candidata = (resultado as? Resultado.Exito)?.datos
                ?.firstOrNull { it.id !in descartadas }
            alTerminar(candidata?.let { Recomendacion(it, MOTIVO_POPULAR) })
        }
    }

    /**
     * Coge una de la lista, saltando las que el usuario ya ha visto o marcado.
     *
     * No se coge siempre la primera: con la semilla se avanza por la lista, así dos días
     * seguidos por la misma vía no dan la misma película.
     */
    private fun elegir(
        resultado: Resultado<List<MovieModel>>,
        semilla: Int,
        descartadas: Set<Int>
    ): MovieModel? {
        val validas = (resultado as? Resultado.Exito)?.datos
            ?.filter { it.id !in descartadas && it.posterPath != null }
            .orEmpty()
        if (validas.isEmpty()) return null
        return validas[semilla % validas.size]
    }

    private enum class Via { GENTE, FAVORITAS, GENEROS, POPULARES }

    companion object {
        const val MOTIVO_GENEROS = "Porque te gusta este tipo de cine"
        const val MOTIVO_POPULAR = "De lo más visto ahora mismo"
    }
}
