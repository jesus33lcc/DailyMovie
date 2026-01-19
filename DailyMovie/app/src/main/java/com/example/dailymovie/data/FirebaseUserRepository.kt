package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.models.SerieModel
import com.google.firebase.auth.EmailAuthProvider
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Guarda y lee del usuario en Firebase.
 *
 * Estructura en Firestore:
 *   users/{uid}                      -> favorites, watched, history (arrays) y tastes (mapa)
 *   users/{uid}/lists/{nombre}       -> movies (array). El id del documento ES el nombre
 *   dailymovie/{id}                  -> la pelicula curada del dia
 *
 * Aqui no se lanza nunca una excepcion hacia fuera: si algo falla, la lectura avisa con la
 * lista vacia y la escritura con un false. Es a proposito, porque en estas pantallas al
 * usuario no le sirve de nada saber si fue la red o las reglas de Firestore.
 *
 * @param auth quien lleva la sesion. Se puede cambiar por un doble en los tests.
 * @param db la base de datos. Igual, se puede sustituir para probar sin tocar Firebase.
 */
class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    init {
        // Persistencia offline: lo que ya se ha visto sigue estando sin cobertura.
        //
        // Se usa setLocalCacheSettings y no el setPersistenceEnabled de toda la vida porque
        // ese quedo obsoleto; hacen lo mismo, pero el nuevo ademas deja elegir el tamaño de
        // la cache si algun dia hace falta.
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
    }

    private fun uid(): String? = auth.currentUser?.uid
    private fun documentoDelUsuario() = uid()?.let { db.collection(USUARIOS).document(it) }

    // ---------------- Cuenta ----------------

    override fun haySesion() = auth.currentUser != null

    override fun correoDelUsuario() = auth.currentUser?.email

    override fun registrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) {
        if (correo.isBlank() || contrasena.isBlank()) {
            alTerminar(false, "El correo y la contraseña no pueden estar vacíos")
            return
        }
        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { alTerminar(true, null) }
            .addOnFailureListener { alTerminar(false, traducirError(it)) }
    }

    override fun entrar(correo: String, contrasena: String, alTerminar: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener { alTerminar(true, null) }
            .addOnFailureListener { alTerminar(false, traducirError(it)) }
    }

    override fun entrarConGoogle(idToken: String, alTerminar: (Boolean, String?) -> Unit) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial)
            .addOnSuccessListener { alTerminar(true, null) }
            .addOnFailureListener { alTerminar(false, traducirError(it)) }
    }

    override fun salir(alTerminar: (Boolean) -> Unit) {
        auth.signOut()
        alTerminar(true)
    }

    override fun mandarCorreoDeRecuperacion(correo: String, alTerminar: (Boolean, String?) -> Unit) {
        if (correo.isBlank()) {
            alTerminar(false, "Escribe tu correo para poder mandarte el enlace")
            return
        }
        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener { alTerminar(true, null) }
            .addOnFailureListener { alTerminar(false, traducirError(it)) }
    }

    override fun cambiarContrasena(actual: String, nueva: String, alTerminar: (Boolean, String) -> Unit) {
        val usuario = auth.currentUser
        val correo = usuario?.email
        if (usuario == null || correo == null) {
            alTerminar(false, "No hay ninguna sesión iniciada")
            return
        }
        // Firebase obliga a volver a identificarse antes de tocar la contraseña.
        usuario.reauthenticate(EmailAuthProvider.getCredential(correo, actual))
            .addOnSuccessListener {
                usuario.updatePassword(nueva)
                    .addOnSuccessListener { alTerminar(true, "Contraseña actualizada") }
                    .addOnFailureListener { alTerminar(false, traducirError(it)) }
            }
            .addOnFailureListener { alTerminar(false, "La contraseña actual no es correcta") }
    }

    override fun borrarCuenta(contrasena: String?, alTerminar: (Boolean, String) -> Unit) {
        val usuario = auth.currentUser
        if (usuario == null) {
            alTerminar(false, "No hay ninguna sesión iniciada")
            return
        }

        // Se borran primero los datos y despues la cuenta: al reves, sin sesion, las reglas
        // de Firestore ya no dejarian tocar el documento y quedaria basura para siempre.
        fun borrarLaCuenta() {
            usuario.delete()
                .addOnSuccessListener { alTerminar(true, "Cuenta y datos borrados") }
                .addOnFailureListener { alTerminar(false, traducirError(it)) }
        }

        fun borrarLosDatos() {
            val documento = documentoDelUsuario()
            if (documento == null) {
                borrarLaCuenta()
                return
            }
            documento.collection(LISTAS).get()
                .addOnSuccessListener { listas ->
                    val borrados = listas.documents.map { it.reference.delete() }
                    // Se espera a que caigan todas las listas antes de ir a por el documento.
                    if (borrados.isEmpty()) {
                        documento.delete().addOnCompleteListener { borrarLaCuenta() }
                    } else {
                        var pendientes = borrados.size
                        borrados.forEach { tarea ->
                            tarea.addOnCompleteListener {
                                pendientes--
                                if (pendientes == 0) {
                                    documento.delete().addOnCompleteListener { borrarLaCuenta() }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { documento.delete().addOnCompleteListener { borrarLaCuenta() } }
        }

        val correo = usuario.email
        // Borrar la cuenta es una operacion delicada: si el usuario entro con correo hay que
        // pedirle la contraseña otra vez. Con Google la sesion reciente ya vale.
        if (contrasena != null && correo != null) {
            usuario.reauthenticate(EmailAuthProvider.getCredential(correo, contrasena))
                .addOnSuccessListener { borrarLosDatos() }
                .addOnFailureListener { alTerminar(false, "La contraseña no es correcta") }
        } else {
            borrarLosDatos()
        }
    }

    // ---------------- Listas ----------------

    override fun favoritas(alTerminar: (List<MovieModel>) -> Unit) = leerLista(FAVORITAS, alTerminar)

    override fun vistas(alTerminar: (List<MovieModel>) -> Unit) = leerLista(VISTAS, alTerminar)

    override fun esFavorita(peliculaId: Int, alTerminar: (Boolean) -> Unit) =
        contiene(FAVORITAS, peliculaId, alTerminar)

    override fun estaVista(peliculaId: Int, alTerminar: (Boolean) -> Unit) =
        contiene(VISTAS, peliculaId, alTerminar)

    override fun ponerFavorita(pelicula: MovieModel, favorita: Boolean, alTerminar: (Boolean) -> Unit) =
        poner(FAVORITAS, pelicula, favorita, alTerminar)

    override fun ponerVista(pelicula: MovieModel, vista: Boolean, alTerminar: (Boolean) -> Unit) =
        poner(VISTAS, pelicula, vista, alTerminar)

    override fun listasDelUsuario(alTerminar: (List<String>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyList())
        documento.collection(LISTAS).get()
            .addOnSuccessListener { alTerminar(it.documents.map { doc -> doc.id }) }
            .addOnFailureListener { alTerminar(emptyList()) }
    }

    override fun crearLista(nombre: String, alTerminar: (AltaDeLista) -> Unit) {
        val limpio = nombre.trim()
        when {
            limpio.isEmpty() -> return alTerminar(AltaDeLista.NOMBRE_VACIO)
            ListaFija.estaReservado(limpio) -> return alTerminar(AltaDeLista.NOMBRE_RESERVADO)
            // El nombre acaba siendo el id del documento, y ahi Firestore no admite barras
            // ni los nombres reservados de un solo punto.
            limpio.contains("/") || limpio == "." || limpio == ".." ->
                return alTerminar(AltaDeLista.NOMBRE_INVALIDO)
        }

        val documento = documentoDelUsuario() ?: return alTerminar(AltaDeLista.SIN_SESION)
        val lista = documento.collection(LISTAS).document(limpio)

        lista.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    alTerminar(AltaDeLista.YA_EXISTE)
                } else {
                    // Firestore aplica la escritura en el movil al momento, pero el Task no
                    // se completa hasta que el servidor la confirma. Si esperasemos a eso,
                    // con mala cobertura el boton de crear no haria nada de nada: ni exito
                    // ni error. Se da por creada aqui, que es lo que el usuario ya ve, y si
                    // el servidor acaba rechazandola se avisa cuando llegue la negativa.
                    lista.set(mapOf(PELICULAS to emptyList<MovieModel>()))
                        .addOnFailureListener { alTerminar(AltaDeLista.RECHAZADA) }
                    alTerminar(AltaDeLista.CREADA)
                }
            }
            .addOnFailureListener { alTerminar(AltaDeLista.RECHAZADA) }
    }

    override fun borrarLista(nombre: String, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        documento.collection(LISTAS).document(nombre).delete()
            .addOnSuccessListener { alTerminar(true) }
            .addOnFailureListener { alTerminar(false) }
    }

    override fun peliculasDeLista(nombre: String, alTerminar: (List<MovieModel>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyList())
        documento.collection(LISTAS).document(nombre).get()
            .addOnSuccessListener { alTerminar(aPeliculas(it.get(PELICULAS))) }
            .addOnFailureListener { alTerminar(emptyList()) }
    }

    override fun listasQueContienen(peliculaId: Int, alTerminar: (Set<String>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptySet())
        // Se baja la subcoleccion entera una sola vez y se mira dentro, en vez de preguntar
        // lista por lista: son pocas y asi es un unico viaje.
        documento.collection(LISTAS).get()
            .addOnSuccessListener { listas ->
                alTerminar(
                    listas.documents
                        .filter { doc -> aPeliculas(doc.get(PELICULAS)).any { it.id == peliculaId } }
                        .map { it.id }
                        .toSet()
                )
            }
            .addOnFailureListener { alTerminar(emptySet()) }
    }

    override fun anadirALista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        val lista = documento.collection(LISTAS).document(nombre)
        lista.get().addOnSuccessListener { doc ->
            val yaEstaba = aPeliculas(doc.get(PELICULAS)).any { it.id == pelicula.id }
            if (yaEstaba) return@addOnSuccessListener alTerminar(false)
            lista.set(mapOf(PELICULAS to FieldValue.arrayUnion(pelicula)), SetOptions.merge())
                .addOnSuccessListener { alTerminar(true) }
                .addOnFailureListener { alTerminar(false) }
        }.addOnFailureListener { alTerminar(false) }
    }

    override fun quitarDeLista(nombre: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        documento.collection(LISTAS).document(nombre)
            .update(PELICULAS, FieldValue.arrayRemove(pelicula))
            .addOnSuccessListener { alTerminar(true) }
            .addOnFailureListener { alTerminar(false) }
    }

    // ---------------- Series ----------------

    override fun seriesFavoritas(alTerminar: (List<SerieModel>) -> Unit) =
        leerSeries(SERIES_FAVORITAS, alTerminar)

    override fun seriesVistas(alTerminar: (List<SerieModel>) -> Unit) =
        leerSeries(SERIES_VISTAS, alTerminar)

    override fun esSerieFavorita(serieId: Int, alTerminar: (Boolean) -> Unit) =
        leerSeries(SERIES_FAVORITAS) { series -> alTerminar(series.any { it.id == serieId }) }

    override fun estaSerieVista(serieId: Int, alTerminar: (Boolean) -> Unit) =
        leerSeries(SERIES_VISTAS) { series -> alTerminar(series.any { it.id == serieId }) }

    override fun ponerSerieFavorita(serie: SerieModel, favorita: Boolean, alTerminar: (Boolean) -> Unit) =
        ponerSerie(SERIES_FAVORITAS, serie, favorita, alTerminar)

    override fun ponerSerieVista(serie: SerieModel, vista: Boolean, alTerminar: (Boolean) -> Unit) =
        ponerSerie(SERIES_VISTAS, serie, vista, alTerminar)

    /**
     * Los episodios vistos se guardan como texto "serie-temporada-episodio" en un solo array.
     *
     * Se eligio asi y no una subcoleccion por serie porque el uso es siempre el mismo: abrir
     * una serie y saber al momento que has visto. Con un array se lee el documento del usuario
     * que ya esta en cache offline y no hace falta ninguna consulta mas; con una subcoleccion
     * habria una lectura por serie abierta.
     */
    override fun episodiosVistos(serieId: Int, alTerminar: (Set<Pair<Int, Int>>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptySet())
        documento.get()
            .addOnSuccessListener { instantanea ->
                val marcas = (instantanea.get(EPISODIOS_VISTOS) as? List<*>).orEmpty()
                alTerminar(
                    marcas.mapNotNull { aEpisodioDeEstaSerie(it, serieId) }.toSet()
                )
            }
            .addOnFailureListener { alTerminar(emptySet()) }
    }

    /**
     * Aqui no hace falta leer antes de escribir, al reves que en [poner].
     *
     * La marca es un texto plano ("1396-1-1"), asi que `arrayRemove` la encuentra siempre: el
     * problema de los favoritos era que comparaba objetos enteros y la nota de TMDB cambiaba.
     * Con esto, meter y sacar son las dos una sola escritura idempotente.
     */
    override fun ponerEpisodioVisto(
        serieId: Int,
        temporada: Int,
        episodio: Int,
        visto: Boolean,
        alTerminar: (Boolean) -> Unit
    ) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        val marca = "$serieId-$temporada-$episodio"
        val cambio = if (visto) FieldValue.arrayUnion(marca) else FieldValue.arrayRemove(marca)
        escribirSinEsperar(documento, mapOf(EPISODIOS_VISTOS to cambio), alTerminar)
    }

    override fun seriesEmpezadas(alTerminar: (Map<Int, Int>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyMap())
        documento.get()
            .addOnSuccessListener { instantanea ->
                val marcas = (instantanea.get(EPISODIOS_VISTOS) as? List<*>).orEmpty()
                alTerminar(
                    marcas.mapNotNull { (it as? String)?.substringBefore("-")?.toIntOrNull() }
                        .groupingBy { it }
                        .eachCount()
                )
            }
            .addOnFailureListener { alTerminar(emptyMap()) }
    }

    /** Descompone "1396-5-14" y se queda con la temporada y el episodio si es de esta serie. */
    private fun aEpisodioDeEstaSerie(marca: Any?, serieId: Int): Pair<Int, Int>? {
        val trozos = (marca as? String)?.split("-") ?: return null
        if (trozos.size != 3 || trozos[0].toIntOrNull() != serieId) return null
        val temporada = trozos[1].toIntOrNull() ?: return null
        val episodio = trozos[2].toIntOrNull() ?: return null
        return temporada to episodio
    }

    override fun seriesDeLista(nombre: String, alTerminar: (List<SerieModel>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyList())
        documento.collection(LISTAS).document(nombre).get()
            .addOnSuccessListener { alTerminar(aSeries(it.get(SERIES))) }
            .addOnFailureListener { alTerminar(emptyList()) }
    }

    override fun anadirSerieALista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        val lista = documento.collection(LISTAS).document(nombre)
        lista.get().addOnSuccessListener { doc ->
            if (aSeries(doc.get(SERIES)).any { it.id == serie.id }) return@addOnSuccessListener alTerminar(false)
            lista.set(mapOf(SERIES to FieldValue.arrayUnion(serie)), SetOptions.merge())
                .addOnSuccessListener { alTerminar(true) }
                .addOnFailureListener { alTerminar(false) }
        }.addOnFailureListener { alTerminar(false) }
    }

    override fun quitarSerieDeLista(nombre: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        documento.collection(LISTAS).document(nombre)
            .update(SERIES, FieldValue.arrayRemove(serie))
            .addOnSuccessListener { alTerminar(true) }
            .addOnFailureListener { alTerminar(false) }
    }

    override fun listasQueContienenSerie(serieId: Int, alTerminar: (Set<String>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptySet())
        documento.collection(LISTAS).get()
            .addOnSuccessListener { listas ->
                alTerminar(
                    listas.documents
                        .filter { doc -> aSeries(doc.get(SERIES)).any { it.id == serieId } }
                        .map { it.id }
                        .toSet()
                )
            }
            .addOnFailureListener { alTerminar(emptySet()) }
    }

    // ---------------- Historial ----------------

    override fun historial(alTerminar: (List<MovieModel>) -> Unit) = leerLista(HISTORIAL, alTerminar)

    /**
     * Mete la pelicula al final del historial, quitandola antes si ya estaba.
     *
     * Las dos escrituras van seguidas sin esperar a la primera: Firestore aplica en orden lo
     * que se le manda al mismo documento, asi que el resultado es el mismo y ademas funciona
     * sin cobertura. Esperando al `addOnCompleteListener` de la primera, sin red la segunda
     * no llegaba a lanzarse nunca y el historial no se guardaba.
     */
    override fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        // El arrayRemove antes del arrayUnion es para que lo visto de nuevo suba al final y
        // el historial quede en orden de visita, sin repetidos.
        documento.set(mapOf(HISTORIAL to FieldValue.arrayRemove(pelicula)), SetOptions.merge())
        escribirSinEsperar(documento, mapOf(HISTORIAL to FieldValue.arrayUnion(pelicula)), alTerminar)
    }

    override fun borrarHistorial(alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        documento.set(mapOf(HISTORIAL to emptyList<MovieModel>()), SetOptions.merge())
            .addOnSuccessListener { alTerminar(true) }
            .addOnFailureListener { alTerminar(false) }
    }

    // ---------------- Gustos ----------------

    override fun guardarGustos(gustos: Gustos, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        val datos = mapOf(
            GUSTOS to mapOf(
                "generos" to gustos.generos,
                "peliculas" to gustos.peliculas,
                "personas" to gustos.personas
            )
        )
        documento.set(datos, SetOptions.merge())
            .addOnSuccessListener { alTerminar(true) }
            .addOnFailureListener { alTerminar(false) }
    }

    override fun gustos(alTerminar: (Gustos?) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(null)
        documento.get()
            .addOnSuccessListener { doc ->
                // Con Map<*, *> no hace falta el @Suppress: las claves se leen igual y no se
                // promete nada sobre los tipos de dentro, que es lo unico que Kotlin no puede
                // comprobar.
                val mapa = doc.get(GUSTOS) as? Map<*, *> ?: return@addOnSuccessListener alTerminar(null)
                alTerminar(
                    Gustos(
                        generos = aEnteros(mapa["generos"]),
                        peliculas = aEnteros(mapa["peliculas"]),
                        personas = aEnteros(mapa["personas"])
                    )
                )
            }
            .addOnFailureListener { alTerminar(null) }
    }

    // ---------------- Pelicula del dia ----------------

    override fun peliculaDelDia(alTerminar: (MovieOfTheDay?) -> Unit) {
        // Antes se descargaba la coleccion entera y se comparaban las fechas ya formateadas
        // en el movil. Ahora se pide solo el documento de hoy, acotando por el dia completo.
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val manana = (hoy.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        db.collection(PELICULA_DEL_DIA)
            .whereGreaterThanOrEqualTo(CAMPO_FECHA, hoy.time)
            .whereLessThan(CAMPO_FECHA, manana.time)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->
                val doc = resultado.documents.firstOrNull() ?: return@addOnSuccessListener alTerminar(null)
                val id = doc.getLong("id")?.toInt()
                val titulo = doc.getString("title")
                val resena = doc.getString("review")
                val fecha = doc.getDate(CAMPO_FECHA)
                val autor = doc.getString("person_name")
                val video = doc.getString("videoId")

                if (id == null || titulo == null || resena == null || fecha == null ||
                    autor == null || video == null
                ) {
                    alTerminar(null)
                } else {
                    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    alTerminar(
                        MovieOfTheDay(
                            id = id,
                            title = titulo,
                            review = resena,
                            date = formato.format(fecha),
                            author = autor,
                            videoId = video
                        )
                    )
                }
            }
            .addOnFailureListener { alTerminar(null) }
    }

    // ---------------- Auxiliares ----------------

    /**
     * Escribe en Firestore sin esperar al servidor.
     *
     * Firestore aplica la escritura en el movil al momento y la sincroniza cuando puede, pero
     * **el Task no se completa hasta que el servidor la confirma**. Esperando a eso, sin
     * cobertura el callback no llega nunca: ni exito ni error. El corazon se quedaba pensando
     * para siempre y dejaba de responder a los toques siguientes.
     *
     * Asi que se da por buena la copia local, que es la que el usuario ya esta viendo. El
     * unico caso en que el servidor puede rechazarla es que las reglas no dejen escribir, y
     * eso seria un fallo de configuracion, no algo del dia a dia; se recoge en el log para
     * poder verlo, pero no se le vuelve a decir nada a quien llamo, que ya se fue hace rato.
     *
     * @param referencia el documento donde se escribe.
     * @param datos lo que se escribe, siempre con merge para no pisar el resto del documento.
     * @param alTerminar se llama **una sola vez** y siempre con true.
     */
    private fun escribirSinEsperar(
        referencia: DocumentReference,
        datos: Map<String, Any>,
        alTerminar: (Boolean) -> Unit
    ) {
        referencia.set(datos, SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Firestore rechazo la escritura", it) }
        alTerminar(true)
    }

    /**
     * Lee una lista de peliculas de un campo del usuario.
     *
     * **Ojo: devuelve lista vacia tanto si no hay nada como si la lectura falla.** Sirve para
     * pintar (una lista vacia se ve igual de bien que un error), pero **no** para decidir que
     * se reescribe: para eso esta [leerListaOFallar].
     */
    /**
     * Lee un campo del documento del usuario y lo convierte a lo que haga falta.
     *
     * Habia cuatro lectores calcados (peliculas y series, cada uno con su version que
     * distingue el fallo) que solo cambiaban en el nombre del campo y en la conversion.
     *
     * @param campo el nombre del array en Firestore.
     * @param convertir como se pasa de lo que guarda Firestore a modelos.
     * @param alTerminar recibe la lista, o **null si la lectura fallo**. Distinguir las dos
     *   cosas importa: confundir "no hay nada" con "no se ha podido leer" es lo que hacia que
     *   quitar una pelicula de favoritos pudiera borrarlos todos.
     */
    private fun <T> leerCampo(
        campo: String,
        convertir: (Any?) -> List<T>,
        alTerminar: (List<T>?) -> Unit
    ) {
        val documento = documentoDelUsuario() ?: return alTerminar(null)
        documento.get()
            .addOnSuccessListener { alTerminar(convertir(it.get(campo))) }
            .addOnFailureListener { alTerminar(null) }
    }

    private fun leerLista(campo: String, alTerminar: (List<MovieModel>) -> Unit) {
        leerCampo(campo, ::aPeliculas) { alTerminar(it.orEmpty()) }
    }

    /**
     * Lo mismo que [leerLista] pero diciendo si de verdad se pudo leer.
     *
     * Hace falta para quitar cosas de una lista: ahi se reescribe el array entero, y confundir
     * "no hay nada" con "no se ha podido leer" significa guardar una lista vacia encima de los
     * favoritos del usuario. Es decir, borrarselos.
     *
     * @param alTerminar recibe null si la lectura fallo, y la lista si salio bien.
     */
    private fun leerListaOFallar(campo: String, alTerminar: (List<MovieModel>?) -> Unit) =
        leerCampo(campo, ::aPeliculas, alTerminar)

    /** Lo mismo que [leerListaOFallar] pero con series. */
    private fun leerSeriesOFallar(campo: String, alTerminar: (List<SerieModel>?) -> Unit) =
        leerCampo(campo, ::aSeries, alTerminar)

    private fun contiene(campo: String, peliculaId: Int, alTerminar: (Boolean) -> Unit) {
        leerLista(campo) { peliculas -> alTerminar(peliculas.any { it.id == peliculaId }) }
    }

    /**
     * Deja la pelicula dentro o fuera de un campo, segun se pida.
     *
     * Los dos caminos son distintos a proposito:
     *
     * - **Meterla** es una sola escritura con `arrayUnion`, que ya es idempotente: si la
     *   pelicula estaba, no hace nada. No hace falta leer antes.
     * - **Sacarla** obliga a leer, porque `arrayRemove` solo quita el elemento si es **igual
     *   campo a campo** al guardado, y la nota de TMDB cambia con el tiempo: la pelicula que
     *   se guardo con un 7,8 hace meses no coincide con la que llega hoy con un 7,9, y el
     *   arrayRemove se quedaba sin hacer nada. Se lee, se quita por id y se reescribe la
     *   lista entera.
     *
     * La lectura de sacar ya no es una carrera: quien llama manda **como tiene que quedar**,
     * no "lo contrario de lo que hay", asi que dos ordenes seguidas acaban en el mismo sitio.
     */
    private fun poner(
        campo: String,
        pelicula: MovieModel,
        dentro: Boolean,
        alTerminar: (Boolean) -> Unit
    ) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        if (dentro) {
            escribirSinEsperar(documento, mapOf(campo to FieldValue.arrayUnion(pelicula)), alTerminar)
            return
        }
        leerListaOFallar(campo) { peliculas ->
            // Si no se ha podido leer no se escribe NADA. Guardar aqui la lista vacia que
            // devolvia el lector de antes borraba los favoritos enteros del usuario.
            if (peliculas == null) return@leerListaOFallar alTerminar(false)
            escribirSinEsperar(
                documento,
                mapOf(campo to peliculas.filterNot { it.id == pelicula.id }),
                alTerminar
            )
        }
    }

    /**
     * Firestore devuelve las peliculas como mapas sueltos, y hay que rehacerlas a mano.
     * Ojo: las claves guardadas son los nombres Kotlin (releaseDate, voteAverage...), no el
     * snake_case de TMDB, porque se guardo el objeto entero con arrayUnion.
     */
    private fun aPeliculas(bruto: Any?): List<MovieModel> = comoMapas(bruto).mapNotNull { mapa ->
        val id = (mapa["id"] as? Number)?.toInt() ?: return@mapNotNull null
        val titulo = mapa["title"] as? String ?: return@mapNotNull null
        MovieModel(
            id = id,
            title = titulo,
            releaseDate = mapa["releaseDate"] as? String ?: "",
            voteAverage = (mapa["voteAverage"] as? Number)?.toDouble() ?: 0.0,
            posterPath = mapa["posterPath"] as? String
        )
    }

    /**
     * Pasa lo que devuelve Firestore a una lista de mapas, sin fiarse de nada.
     *
     * Antes se hacia `bruto as? List<Map<String, Any?>>`, y ese `as?` **solo comprueba que es
     * una List**: lo de dentro no lo mira nadie, porque los genericos de Kotlin se borran al
     * compilar. Con un dato antiguo o tocado a mano en la consola de Firebase, el primer
     * `mapa["id"]` lanzaba ClassCastException dentro del callback y tiraba la app. El
     * `@Suppress("UNCHECKED_CAST")` que habia justo encima tapaba exactamente eso.
     */
    private fun comoMapas(bruto: Any?): List<Map<*, *>> =
        (bruto as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }

    private fun leerSeries(campo: String, alTerminar: (List<SerieModel>) -> Unit) {
        leerCampo(campo, ::aSeries) { alTerminar(it.orEmpty()) }
    }

    /** Mete o saca la serie segun si ya estaba, igual que con las peliculas. */
    /** Lo mismo que [poner] pero con series. Mismos dos caminos y por las mismas razones. */
    private fun ponerSerie(
        campo: String,
        serie: SerieModel,
        dentro: Boolean,
        alTerminar: (Boolean) -> Unit
    ) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        if (dentro) {
            escribirSinEsperar(documento, mapOf(campo to FieldValue.arrayUnion(serie)), alTerminar)
            return
        }
        leerSeriesOFallar(campo) { series ->
            if (series == null) return@leerSeriesOFallar alTerminar(false)
            escribirSinEsperar(
                documento,
                mapOf(campo to series.filterNot { it.id == serie.id }),
                alTerminar
            )
        }
    }

    /**
     * Las series guardadas, rehechas a mano igual que las peliculas.
     *
     * Ojo con los nombres: al guardar el objeto entero con arrayUnion, las claves que quedan
     * en Firestore son las de Kotlin (titulo, estreno, valoracion, poster), no las de TMDB.
     */
    private fun aSeries(bruto: Any?): List<SerieModel> = comoMapas(bruto).mapNotNull { mapa ->
        val id = (mapa["id"] as? Number)?.toInt() ?: return@mapNotNull null
        val titulo = mapa["titulo"] as? String ?: return@mapNotNull null
        SerieModel(
            id = id,
            titulo = titulo,
            estreno = mapa["estreno"] as? String,
            valoracion = (mapa["valoracion"] as? Number)?.toDouble() ?: 0.0,
            poster = mapa["poster"] as? String
        )
    }

    private fun aEnteros(bruto: Any?): List<Int> =
        (bruto as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }

    /** Los mensajes de Firebase vienen en ingles; se cambian por algo entendible. */
    private fun traducirError(error: Exception): String = when {
        error.message?.contains("password is invalid", true) == true ||
            error.message?.contains("credential is incorrect", true) == true ->
            "El correo o la contraseña no son correctos"
        error.message?.contains("email address is already", true) == true ->
            "Ya hay una cuenta con ese correo"
        error.message?.contains("badly formatted", true) == true ->
            "Ese correo no tiene buena pinta, revísalo"
        error.message?.contains("at least 6 characters", true) == true ->
            "La contraseña necesita al menos 6 caracteres"
        error.message?.contains("no user record", true) == true ->
            "No hay ninguna cuenta con ese correo"
        error.message?.contains("network", true) == true ->
            "No hay conexión. Revisa tu internet"
        error.message?.contains("recent", true) == true ->
            "Por seguridad, vuelve a iniciar sesión antes de hacer esto"
        else -> "Algo ha fallado, inténtalo de nuevo"
    }

    private companion object {
        const val TAG = "DailyMovie/Firestore"
        const val USUARIOS = "users"
        const val LISTAS = "lists"
        const val PELICULAS = "movies"
        const val FAVORITAS = "favorites"
        const val VISTAS = "watched"
        const val HISTORIAL = "history"
        const val SERIES = "series"
        const val SERIES_FAVORITAS = "favoriteSeries"
        const val SERIES_VISTAS = "watchedSeries"
        const val EPISODIOS_VISTOS = "watchedEpisodes"
        const val GUSTOS = "tastes"
        const val PELICULA_DEL_DIA = "dailymovie"
        const val CAMPO_FECHA = "date"
    }
}
