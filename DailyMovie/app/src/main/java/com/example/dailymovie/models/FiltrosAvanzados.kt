package com.example.dailymovie.models

/**
 * Los filtros finos del buscador.
 *
 * Van todos a null por defecto y eso significa "me da igual": las consultas nulas ni se
 * mandan a TMDB, asi que quien no filtra no paga por ello.
 *
 * @property ano solo lo estrenado ese año.
 * @property notaMinima de 0 a 10. Ojo: una nota alta con pocos votos no dice gran cosa, por
 *   eso discover ya pide de serie un minimo de votos.
 * @property plataforma id de TMDB de la plataforma de streaming. No es el mismo en todos los
 *   paises, por eso se piden las del pais del aparato en vez de tener una lista fija.
 * @property nombrePlataforma solo para poder enseñarlo en el chip; no se manda a TMDB.
 */
data class FiltrosAvanzados(
    val ano: Int? = null,
    val notaMinima: Double? = null,
    val plataforma: Int? = null,
    val nombrePlataforma: String? = null
) {
    /** Si no hay ninguno puesto, se puede seguir usando search en vez de discover. */
    fun estanVacios() = ano == null && notaMinima == null && plataforma == null

    /** Cuantos hay puestos, para poder avisarlo en el boton. */
    fun cuantos() = listOfNotNull(ano, notaMinima, plataforma).size
}
