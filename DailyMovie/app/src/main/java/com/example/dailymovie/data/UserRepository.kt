package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay

/**
 * Las dos listas que trae la app de serie.
 *
 * Antes se comparaba el texto "Favoritos" y "Vistos" a mano en ListasF, ListMoviesA y los
 * dialogos, asi que una lista personalizada con ese nombre chocaba con ellas y ademas habia
 * que acordarse de escribirlo igual en todos los sitios. Con esto el compilador avisa.
 */
enum class ListaFija(val titulo: String) {
    FAVORITOS("Favoritos"),
    VISTOS("Vistos");

    companion object {
        /** Devuelve la lista fija que corresponde a ese nombre, o null si es una del usuario. */
        fun desdeTitulo(titulo: String): ListaFija? = entries.find { it.titulo == titulo }

        /** Para impedir que el usuario cree una lista que choque con las de serie. */
        fun estaReservado(titulo: String): Boolean = desdeTitulo(titulo) != null
    }
}

/**
 * Todo lo que la app guarda del usuario: cuenta, listas, historial y gustos.
 *
 * Igual que MovieRepository, es una interfaz para que los ViewModels no dependan del object
 * FirebaseClient y se puedan probar con un doble.
 */
interface UserRepository {

    // ---- Cuenta ----
    fun haySesion(): Boolean
    fun correoDelUsuario(): String?
    fun registrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit)
    fun entrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit)
    fun entrarConGoogle(idToken: String, alTerminar: (Boolean, String?) -> Unit)
    fun salir(alTerminar: (Boolean) -> Unit)
    fun mandarCorreoDeRecuperacion(correo: String, alTerminar: (Boolean, String?) -> Unit)
    fun cambiarContrasena(actual: String, nueva: String, alTerminar: (Boolean, String) -> Unit)

    /** Borra los datos del usuario y despues la cuenta. Obligatorio para Google Play. */
    fun borrarCuenta(contrasena: String?, alTerminar: (Boolean, String) -> Unit)

    // ---- Listas ----
    fun favoritas(alTerminar: (List<MovieModel>) -> Unit)
    fun vistas(alTerminar: (List<MovieModel>) -> Unit)
    fun esFavorita(peliculaId: Int, alTerminar: (Boolean) -> Unit)
    fun estaVista(peliculaId: Int, alTerminar: (Boolean) -> Unit)
    fun cambiarFavorita(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)
    fun cambiarVista(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    fun listasDelUsuario(alTerminar: (List<String>) -> Unit)
    fun crearLista(nombre: String, alTerminar: (AltaDeLista) -> Unit)
    fun borrarLista(nombre: String, alTerminar: (Boolean) -> Unit)
    fun peliculasDeLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit)
    fun anadirALista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit)
    fun quitarDeLista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    // ---- Historial ----
    fun historial(alTerminar: (List<MovieModel>) -> Unit)
    fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)
    fun borrarHistorial(alTerminar: (Boolean) -> Unit)

    // ---- Gustos (onboarding) ----
    fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit)
    fun gustos(alTerminar: (Gustos?) -> Unit)

    // ---- Pelicula del dia ----
    fun peliculaDelDia(alTerminar: (MovieOfTheDay?) -> Unit)
}

/**
 * Como ha ido crear una lista.
 *
 * Antes esto era un Boolean, y la pantalla enseñaba "la lista ya existe" ante cualquier
 * fallo: nombre reservado, sesion caducada o un rechazo de Firestore se veian todos igual,
 * asi que era imposible saber que pasaba de verdad.
 */
enum class AltaDeLista {
    CREADA,
    NOMBRE_VACIO,

    /** El nombre choca con Favoritos o Vistos. */
    NOMBRE_RESERVADO,

    /** Firestore usa el nombre como id de documento y ahi la barra no vale. */
    NOMBRE_INVALIDO,
    YA_EXISTE,
    SIN_SESION,

    /** Firestore la rechazo: normalmente son las reglas de seguridad. */
    RECHAZADA
}

/**
 * Lo que el usuario dice que le gusta al registrarse.
 *
 * Se guardan solo los ids de TMDB, que es lo que hace falta para pedir recomendaciones.
 */
data class Gustos(
    val generos: List<Int> = emptyList(),
    val peliculas: List<Int> = emptyList(),
    val personas: List<Int> = emptyList()
) {
    /** Si no ha elegido nada no se puede recomendar, y toca enseñar lo popular. */
    fun estanVacios() = generos.isEmpty() && peliculas.isEmpty() && personas.isEmpty()
}
