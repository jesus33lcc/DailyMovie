package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.models.SerieModel

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
        /**
         * Devuelve la lista fija que corresponde a ese nombre, o null si es una del usuario.
         *
         * @param titulo el nombre tal cual, respetando mayusculas: "favoritos" no cuenta.
         */
        fun desdeTitulo(titulo: String): ListaFija? = entries.find { it.titulo == titulo }

        /**
         * Para impedir que el usuario cree una lista que choque con las de serie.
         *
         * @param titulo el nombre que quiere ponerle, ya sin espacios sobrantes.
         * @return true si ese nombre esta pillado por una lista fija.
         */
        fun estaReservado(titulo: String): Boolean = desdeTitulo(titulo) != null
    }
}

/**
 * Todo lo que la app guarda del usuario: cuenta, listas, historial y gustos.
 *
 * Igual que MovieRepository, es una interfaz para que los ViewModels no dependan del object
 * FirebaseClient y se puedan probar con un doble.
 *
 * Aqui no hay [Resultado] como en TMDB: las lecturas avisan con lo que hayan encontrado y las
 * escrituras con un simple si o no. Eso quiere decir que **una lista vacia o un false no
 * distinguen entre "no hay nada", "no hay sesion" y "Firestore lo rechazo"**. Se dejo asi
 * porque en estas pantallas el usuario no puede hacer nada distinto en cada caso; la excepcion
 * es crear listas, que si tiene su propio enum ([AltaDeLista]) porque ahi el motivo importa.
 *
 * Firestore guarda en el movil lo que ya se ha leido, asi que sin cobertura sigue contestando
 * con lo ultimo que sabia en vez de quedarse colgado.
 */
interface UserRepository {

    // ---- Cuenta ----

    /** Si hay alguien con la sesion abierta. Es lo que mira MainActivity para saber a donde ir. */
    fun haySesion(): Boolean

    /** El correo del usuario, o null si no hay sesion. Se enseña en ajustes. */
    fun correoDelUsuario(): String?

    /**
     * Crea una cuenta nueva con correo y contraseña.
     *
     * @param correo el que ha escrito el usuario.
     * @param contrasena la que ha escrito. Firebase pide al menos 6 caracteres.
     * @param alTerminar recibe si salio bien y, si no, el motivo ya traducido a español y
     *   listo para enseñar: los mensajes de Firebase vienen en ingles y no se entienden.
     */
    fun registrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit)

    /**
     * Abre sesion con correo y contraseña.
     *
     * @param correo el que ha escrito el usuario.
     * @param contrasena la que ha escrito.
     * @param alTerminar recibe si salio bien y, si no, el motivo ya en español. A proposito no
     *   dice si lo que falla es el correo o la contraseña: eso serviria para adivinar quien
     *   tiene cuenta.
     */
    fun entrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit)

    /**
     * Abre sesion con la cuenta de Google que el usuario haya elegido.
     *
     * @param idToken el que devuelve la pantalla de Google Sign-In. Aqui ya no se le pregunta
     *   nada al usuario, solo se canjea.
     * @param alTerminar recibe si salio bien y, si no, el motivo ya en español.
     */
    fun entrarConGoogle(idToken: String, alTerminar: (Boolean, String?) -> Unit)

    /**
     * Cierra la sesion.
     *
     * @param alTerminar recibe siempre true: cerrar sesion es cosa del movil, no hay que
     *   pedirle permiso a ningun servidor y no puede fallar.
     */
    fun salir(alTerminar: (Boolean) -> Unit)

    /**
     * Manda el correo con el enlace para poner una contraseña nueva.
     *
     * @param correo a donde se manda.
     * @param alTerminar recibe si se pudo mandar y, si no, el motivo ya en español.
     */
    fun mandarCorreoDeRecuperacion(correo: String, alTerminar: (Boolean, String?) -> Unit)

    /**
     * Cambia la contraseña del usuario que tiene la sesion abierta.
     *
     * @param actual la de ahora. Firebase obliga a volver a identificarse antes de tocar la
     *   contraseña, aunque la sesion este abierta.
     * @param nueva la que quiere poner.
     * @param alTerminar recibe si salio bien y un mensaje que hay siempre, tanto para decir
     *   que se cambio como para explicar por que no.
     */
    fun cambiarContrasena(actual: String, nueva: String, alTerminar: (Boolean, String) -> Unit)

    /**
     * Borra los datos del usuario y despues la cuenta. Obligatorio para Google Play.
     *
     * @param contrasena la del usuario, para confirmar que es el. Va a null si entro con
     *   Google, que ahi no hay contraseña que pedir y basta con que la sesion sea reciente.
     * @param alTerminar recibe si se borro todo y un mensaje que hay siempre, para enseñar.
     */
    fun borrarCuenta(contrasena: String?, alTerminar: (Boolean, String) -> Unit)

    // ---- Listas ----

    /** @param alTerminar recibe las peliculas marcadas como favoritas, o una lista vacia. */
    fun favoritas(alTerminar: (List<MovieModel>) -> Unit)

    /** @param alTerminar recibe las peliculas marcadas como vistas, o una lista vacia. */
    fun vistas(alTerminar: (List<MovieModel>) -> Unit)

    /**
     * Si esa pelicula esta en favoritos.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe true solo si esta. Un false tambien puede significar que no se
     *   pudo mirar, asi que sirve para pintar el corazon pero no para decidir nada delicado.
     */
    fun esFavorita(peliculaId: Int, alTerminar: (Boolean) -> Unit)

    /**
     * Si esa pelicula esta marcada como vista.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe true solo si esta marcada.
     */
    fun estaVista(peliculaId: Int, alTerminar: (Boolean) -> Unit)

    /**
     * Mete la pelicula en favoritos, o la saca si ya estaba.
     *
     * @param pelicula la pelicula entera, no solo el id: en Firestore se guarda el objeto
     *   completo para poder pintar la lista sin volver a preguntarle a TMDB.
     * @param alTerminar recibe si se pudo guardar el cambio, no en que estado ha quedado. Para
     *   saber eso hay que volver a preguntar con [esFavorita].
     */
    fun cambiarFavorita(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Marca la pelicula como vista, o le quita la marca si ya la tenia.
     *
     * @param pelicula la pelicula entera, por lo mismo que en [cambiarFavorita].
     * @param alTerminar recibe si se pudo guardar el cambio.
     */
    fun cambiarVista(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Los nombres de las listas que se ha creado el usuario.
     *
     * @param alTerminar recibe solo los nombres, sin las peliculas de dentro. Favoritos y
     *   Vistos no salen aqui: esas no son documentos, son campos del usuario.
     */
    fun listasDelUsuario(alTerminar: (List<String>) -> Unit)

    /**
     * Crea una lista nueva.
     *
     * @param nombre el que ha escrito el usuario. Se le quitan los espacios de los lados, y
     *   acaba siendo el id del documento en Firestore, asi que no puede llevar barras ni ser
     *   un nombre reservado.
     * @param alTerminar recibe como fue, con el motivo exacto si no se pudo. Ojo: un CREADA
     *   no garantiza que el servidor la haya aceptado todavia, solo que ya se ve en el movil;
     *   si acaba rechazandola, llega despues una segunda llamada con RECHAZADA.
     */
    fun crearLista(nombre: String, alTerminar: (AltaDeLista) -> Unit)

    /**
     * Borra una lista entera con lo que tenga dentro.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param alTerminar recibe si se pudo borrar.
     */
    fun borrarLista(nombre: String, alTerminar: (Boolean) -> Unit)

    /**
     * Las peliculas guardadas en una lista del usuario.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param alTerminar recibe las peliculas, o una lista vacia si la lista no existe, esta
     *   vacia o no se pudo leer.
     */
    fun peliculasDeLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit)

    /**
     * En cuales de las listas del usuario esta ya esa pelicula.
     *
     * Hace falta para poder abrir el dialogo de añadir con las casillas ya marcadas, en vez
     * de enseñar todas vacias y que el usuario no sepa donde la tiene guardada.
     *
     * @param peliculaId el id de TMDB de la pelicula.
     * @param alTerminar recibe los nombres de las listas donde esta, o un conjunto vacio.
     */
    fun listasQueContienen(peliculaId: Int, alTerminar: (Set<String>) -> Unit)

    /**
     * Añade una pelicula a una lista del usuario.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param pelicula la pelicula entera, que es lo que se guarda.
     * @param alTerminar recibe false tanto si fallo como si la pelicula ya estaba: no se
     *   duplica, y para el usuario el resultado es el mismo, que no hay nada nuevo que añadir.
     */
    fun anadirALista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Saca una pelicula de una lista.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param pelicula la pelicula entera. Tiene que ser igual campo por campo a la guardada, o
     *   Firestore no la reconoce dentro del array y no borra nada.
     * @param alTerminar recibe si se pudo quitar.
     */
    fun quitarDeLista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    // ---- Series ----
    //
    // Van en campos aparte de las peliculas (favoriteSeries, watchedSeries, y el campo
    // "series" dentro de cada lista) porque el dato no es el mismo: una serie tiene fecha de
    // primera emision y no de estreno, y meterlas en el mismo array obligaria a adivinar que
    // es cada cosa al leer.

    /** @param alTerminar recibe las series marcadas como favoritas, o una lista vacia. */
    fun seriesFavoritas(alTerminar: (List<SerieModel>) -> Unit)

    /** @param alTerminar recibe las series marcadas como vistas, o una lista vacia. */
    fun seriesVistas(alTerminar: (List<SerieModel>) -> Unit)

    /**
     * Si esa serie esta en favoritas.
     *
     * @param serieId el id de TMDB de la serie, que no tiene nada que ver con el de pelicula.
     * @param alTerminar recibe true solo si esta.
     */
    fun esSerieFavorita(serieId: Int, alTerminar: (Boolean) -> Unit)

    /**
     * Si esa serie esta marcada como vista.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe true solo si esta marcada.
     */
    fun estaSerieVista(serieId: Int, alTerminar: (Boolean) -> Unit)

    /**
     * Mete la serie en favoritas, o la saca si ya estaba.
     *
     * @param serie la serie entera, que es lo que se guarda.
     * @param alTerminar recibe si se pudo guardar el cambio, no en que estado ha quedado.
     */
    fun cambiarSerieFavorita(serie: SerieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Marca la serie como vista, o le quita la marca.
     *
     * @param serie la serie entera.
     * @param alTerminar recibe si se pudo guardar el cambio.
     */
    fun cambiarSerieVista(serie: SerieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Los episodios que el usuario ya ha visto de una serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe los episodios marcados como pares (temporada, episodio). Llega
     *   vacio tanto si no ha visto ninguno como si no hay sesion o falla la lectura.
     */
    fun episodiosVistos(serieId: Int, alTerminar: (Set<Pair<Int, Int>>) -> Unit)

    /**
     * Las series que el usuario ha empezado, con cuantos episodios lleva de cada una.
     *
     * @param alTerminar recibe un mapa de id de serie a numero de episodios marcados. Llega
     *   vacio si no ha empezado ninguna, si no hay sesion o si falla la lectura.
     */
    fun seriesEmpezadas(alTerminar: (Map<Int, Int>) -> Unit)

    /**
     * Marca o desmarca un episodio, segun como este ahora.
     *
     * @param serieId el id de TMDB de la serie.
     * @param temporada el numero de temporada que da TMDB. La 0 son los especiales.
     * @param episodio el numero de episodio dentro de esa temporada.
     * @param alTerminar recibe true si el episodio queda marcado como visto y false si queda
     *   sin ver. Si la escritura falla, se devuelve el estado que ya tenia.
     */
    fun cambiarEpisodioVisto(
        serieId: Int,
        temporada: Int,
        episodio: Int,
        alTerminar: (Boolean) -> Unit
    )

    /**
     * Las series guardadas en una lista del usuario.
     *
     * Es la misma lista que la de peliculas, pero mirando otro campo: una lista puede tener
     * las dos cosas dentro.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param alTerminar recibe las series, o una lista vacia.
     */
    fun seriesDeLista(nombre: String, alTerminar: (List<SerieModel>) -> Unit)

    /**
     * Añade una serie a una lista del usuario.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param serie la serie entera, que es lo que se guarda.
     * @param alTerminar recibe false tanto si fallo como si la serie ya estaba.
     */
    fun anadirSerieALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Saca una serie de una lista.
     *
     * @param nombre el nombre de la lista, que es su id.
     * @param serie la serie entera, igual campo por campo a la guardada o Firestore no la
     *   encuentra dentro del array.
     * @param alTerminar recibe si se pudo quitar.
     */
    fun quitarSerieDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit)

    /**
     * En cuales de las listas del usuario esta ya esa serie.
     *
     * @param serieId el id de TMDB de la serie.
     * @param alTerminar recibe los nombres de las listas donde esta, o un conjunto vacio.
     */
    fun listasQueContienenSerie(serieId: Int, alTerminar: (Set<String>) -> Unit)

    // ---- Historial ----

    /**
     * Las peliculas que el usuario ha ido abriendo.
     *
     * @param alTerminar recibe el historial en orden de visita, lo mas reciente al final.
     */
    fun historial(alTerminar: (List<MovieModel>) -> Unit)

    /**
     * Apunta que el usuario ha abierto esta pelicula.
     *
     * Si ya estaba, sube al final en vez de repetirse: interesa cuando la vio por ultima vez,
     * no cuantas veces.
     *
     * @param pelicula la pelicula entera, que es lo que se guarda.
     * @param alTerminar recibe si se pudo apuntar.
     */
    fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit)

    /**
     * Vacia el historial entero.
     *
     * @param alTerminar recibe si se pudo vaciar. No toca ni favoritos ni vistos.
     */
    fun borrarHistorial(alTerminar: (Boolean) -> Unit)

    // ---- Gustos (onboarding) ----

    /**
     * Guarda lo que el usuario ha elegido en el onboarding.
     *
     * @param gustos generos, peliculas y gente, solo los ids.
     * @param alTerminar recibe si se pudo guardar. Se pisa lo que hubiera antes, no se suma:
     *   el onboarding se puede repetir desde ajustes y ahi el usuario espera empezar de cero.
     */
    fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit)

    /**
     * Lo que el usuario dijo que le gustaba.
     *
     * @param alTerminar recibe los gustos, o null si nunca hizo el onboarding o no se pudieron
     *   leer. Un null aqui es lo que hace que la portada se caiga a lo popular.
     */
    fun gustos(alTerminar: (Gustos?) -> Unit)

    // ---- Pelicula del dia ----

    /**
     * La pelicula que se ha elegido a mano para hoy.
     *
     * @param alTerminar recibe la del dia, o null si hoy no hay ninguna puesta. Ese null es lo
     *   normal, no un error: la mayoria de los dias no hay ninguna curada y la portada pasa a
     *   recomendar. Tambien llega null si al documento le falta algun campo, que a medias no
     *   se puede pintar.
     */
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
