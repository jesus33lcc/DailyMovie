package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel

/**
 * Una propuesta para la portada: la película y por qué se le enseña.
 *
 * El motivo no es adorno. Una recomendación sin explicación parece un anuncio; con ella el
 * usuario entiende que la app le ha entendido, y si no le encaja sabe qué cambiar en sus
 * gustos.
 */
data class Recomendacion(val pelicula: MovieModel, val motivo: MotivoDeRecomendacion)

/**
 * Por qué se le enseña esa película.
 *
 * Era una cadena en castellano fabricada aquí ("Porque sigues a Nolan"), o sea texto de
 * pantalla dentro de la capa de datos: no se podía traducir ni probar sin comparar frases.
 * Ahora el recomendador dice **qué** motivo es y el texto lo pone la vista, igual que con
 * `ErrorCarga`.
 *
 * @property nombre el actor, el director o la película de la que sale la propuesta. Null en
 *   los motivos que no hablan de nada concreto.
 */
data class MotivoDeRecomendacion(val tipo: TipoDeMotivo, val nombre: String? = null)

/** Por qué vía salió la recomendación. */
enum class TipoDeMotivo {
    /** De alguien que el usuario sigue. Lleva su nombre. */
    GENTE_QUE_SIGUES,

    /** De alguien que sigue, pero no se pudo saber cómo se llama. */
    GENTE_SIN_NOMBRE,

    /** Parecida a una de sus favoritas. Lleva el título de esa. */
    COMO_TU_FAVORITA,

    /** De los géneros que marcó en el onboarding. */
    TUS_GENEROS,

    /** Lo más visto del momento, para quien no ha dicho nada. */
    LO_POPULAR
}

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
                usuario.favoritas { favoritas ->
                    // Nada de lo que el usuario ya tiene guardado o ya ha visto: recomendarle
                    // su propia lista de favoritos sería quedar en ridículo.
                    val descartadas = vistas.map { it.id }.toSet() +
                        favoritas.map { it.id } +
                        gustos?.peliculas.orEmpty()
                    val guion = ordenDeVias(semilla, gustos, favoritas.isNotEmpty())
                    intentar(guion, 0, semilla, gustos, favoritas, descartadas, alTerminar)
                }
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
    private fun ordenDeVias(semilla: Int, gustos: Gustos?, hayFavoritas: Boolean): List<Via> {
        val personales = mutableListOf<Via>()
        if (!gustos?.personas.isNullOrEmpty()) personales += Via.GENTE
        if (!gustos?.generos.isNullOrEmpty()) personales += Via.GENEROS
        // Sin favoritos esta vía no puede dar nada, y si entrara igualmente se comería un
        // turno de cada tres para acabar cayendo a la siguiente.
        if (hayFavoritas) personales += Via.FAVORITAS

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
        favoritas: List<MovieModel>,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        if (cual >= guion.size) {
            alTerminar(null)
            return
        }
        val siguiente = {
            intentar(guion, cual + 1, semilla, gustos, favoritas, descartadas, alTerminar)
        }

        when (guion[cual]) {
            Via.GENTE -> porLaGente(semilla, gustos, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.FAVORITAS -> porFavoritas(semilla, favoritas, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.GENEROS -> porGeneros(semilla, gustos, descartadas) { it?.let(alTerminar) ?: siguiente() }
            Via.POPULARES -> porPopulares(semilla, descartadas) { it?.let(alTerminar) ?: siguiente() }
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
                        if (nombre.isNullOrBlank()) {
                            MotivoDeRecomendacion(TipoDeMotivo.GENTE_SIN_NOMBRE)
                        } else {
                            MotivoDeRecomendacion(TipoDeMotivo.GENTE_QUE_SIGUES, nombre)
                        }
                    )
                )
            }
        }
    }

    private fun porFavoritas(
        semilla: Int,
        favoritas: List<MovieModel>,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        if (favoritas.isEmpty()) {
            alTerminar(null)
            return
        }
        val referencia = favoritas[semilla % favoritas.size]
        peliculas.recomendadas(referencia.id) { resultado ->
            val candidata = elegir(resultado, semilla, descartadas)
            alTerminar(
                candidata?.let {
                    Recomendacion(
                        it,
                        MotivoDeRecomendacion(TipoDeMotivo.COMO_TU_FAVORITA, referencia.title)
                    )
                }
            )
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
            alTerminar(candidata?.let { Recomendacion(it, MotivoDeRecomendacion(TipoDeMotivo.TUS_GENEROS)) })
        }
    }

    private fun porPopulares(
        semilla: Int,
        descartadas: Set<Int>,
        alTerminar: (Recomendacion?) -> Unit
    ) {
        peliculas.populares { resultado ->
            // Pasa por el mismo filtro que las demás: esta vía existe para que la portada
            // nunca falle, así que es la última que puede permitirse devolver algo sin cartel.
            val candidata = elegir(resultado, semilla, descartadas)
            alTerminar(candidata?.let { Recomendacion(it, MotivoDeRecomendacion(TipoDeMotivo.LO_POPULAR)) })
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

}
