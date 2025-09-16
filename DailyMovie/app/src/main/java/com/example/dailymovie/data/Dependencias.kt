package com.example.dailymovie.data

/**
 * De donde salen los repositorios.
 *
 * Es inyeccion de dependencias a mano, sin Hilt ni Koin: para una app de este tamaño una
 * librería entera sería mucho aparato para lo que resuelve. Lo importante ya está: los
 * ViewModels reciben las interfaces por constructor, así que en los tests se les puede
 * pasar un doble sin tocar Firebase ni internet.
 */
object Dependencias {

    /** Se crean una sola vez porque no guardan estado y abrir Firestore cuesta. */
    val peliculas: MovieRepository by lazy { TmdbMovieRepository() }
    val usuario: UserRepository by lazy { FirebaseUserRepository() }
    val personas: PersonRepository by lazy { TmdbPersonRepository() }
    val series: SerieRepository by lazy { TmdbSerieRepository() }
}
