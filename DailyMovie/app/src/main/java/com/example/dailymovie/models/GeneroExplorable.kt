package com.example.dailymovie.models

/**
 * Un genero de los que se pueden explorar, ya con sus dos identidades.
 *
 * TMDB tiene listas de generos distintas para cine y para television, y no coinciden ni en
 * los ids ni en los nombres. "Accion" es el 28 en peliculas, pero en series no existe por
 * separado: esta dentro de "Action & Adventure", que es el 10759. Ademas TMDB no traduce los
 * generos de TV, asi que llegan en ingles aunque se le pida en español.
 *
 * Aqui se juntan los dos: un chip enseña un solo nombre y por dentro sabe a que pedir en cada
 * sitio. Si un genero solo existe en uno de los dos mundos, el otro id se queda a null y esa
 * mitad simplemente no se pide.
 */
data class GeneroExplorable(
    val nombre: String,
    val idPelicula: Int?,
    val idSerie: Int?
) {
    companion object {
        /**
         * Los generos de TV que agrupan varios de cine, o que TMDB deja en ingles.
         *
         * El resto casan solos por nombre (Comedia, Drama, Crimen, Animacion...), asi que
         * aqui solo estan los que no.
         */
        val PUENTE_A_SERIES = mapOf(
            "acción" to 10759,          // Action & Adventure
            "aventura" to 10759,
            "ciencia ficción" to 10765, // Sci-Fi & Fantasy
            "fantasía" to 10765,
            "bélica" to 10768,          // War & Politics
            "guerra" to 10768
        )
    }
}
