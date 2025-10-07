package com.example.dailymovie.data

import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.models.SerieModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
 */
class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    init {
        // Persistencia offline: lo que ya se ha visto sigue estando sin cobertura.
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
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

    override fun cambiarFavorita(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) =
        alternar(FAVORITAS, pelicula, alTerminar)

    override fun cambiarVista(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) =
        alternar(VISTAS, pelicula, alTerminar)

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

    override fun cambiarSerieFavorita(serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        alternarSerie(SERIES_FAVORITAS, serie, alTerminar)

    override fun cambiarSerieVista(serie: SerieModel, alTerminar: (Boolean) -> Unit) =
        alternarSerie(SERIES_VISTAS, serie, alTerminar)

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

    override fun anadirAlHistorial(pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        // arrayRemove antes de arrayUnion para que lo visto de nuevo suba al final y el
        // historial quede en orden de visita, sin duplicados.
        documento.set(mapOf(HISTORIAL to FieldValue.arrayRemove(pelicula)), SetOptions.merge())
            .addOnCompleteListener {
                documento.set(mapOf(HISTORIAL to FieldValue.arrayUnion(pelicula)), SetOptions.merge())
                    .addOnSuccessListener { alTerminar(true) }
                    .addOnFailureListener { alTerminar(false) }
            }
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
                @Suppress("UNCHECKED_CAST")
                val mapa = doc.get(GUSTOS) as? Map<String, Any> ?: return@addOnSuccessListener alTerminar(null)
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
                    alTerminar(MovieOfTheDay(id, titulo, resena, formato.format(fecha), autor, video))
                }
            }
            .addOnFailureListener { alTerminar(null) }
    }

    // ---------------- Auxiliares ----------------

    private fun leerLista(campo: String, alTerminar: (List<MovieModel>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyList())
        documento.get()
            .addOnSuccessListener { alTerminar(aPeliculas(it.get(campo))) }
            .addOnFailureListener { alTerminar(emptyList()) }
    }

    private fun contiene(campo: String, peliculaId: Int, alTerminar: (Boolean) -> Unit) {
        leerLista(campo) { peliculas -> alTerminar(peliculas.any { it.id == peliculaId }) }
    }

    /** Mete o saca la pelicula segun si ya estaba, y avisa de si la operacion salio bien. */
    private fun alternar(campo: String, pelicula: MovieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        contiene(campo, pelicula.id) { yaEstaba ->
            val cambio = if (yaEstaba) FieldValue.arrayRemove(pelicula) else FieldValue.arrayUnion(pelicula)
            documento.set(mapOf(campo to cambio), SetOptions.merge())
                .addOnSuccessListener { alTerminar(true) }
                .addOnFailureListener { alTerminar(false) }
        }
    }

    /**
     * Firestore devuelve las peliculas como mapas sueltos, y hay que rehacerlas a mano.
     * Ojo: las claves guardadas son los nombres Kotlin (releaseDate, voteAverage...), no el
     * snake_case de TMDB, porque se guardo el objeto entero con arrayUnion.
     */
    private fun aPeliculas(bruto: Any?): List<MovieModel> {
        @Suppress("UNCHECKED_CAST")
        val lista = bruto as? List<Map<String, Any?>> ?: return emptyList()
        return lista.mapNotNull { mapa ->
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
    }

    private fun leerSeries(campo: String, alTerminar: (List<SerieModel>) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(emptyList())
        documento.get()
            .addOnSuccessListener { alTerminar(aSeries(it.get(campo))) }
            .addOnFailureListener { alTerminar(emptyList()) }
    }

    /** Mete o saca la serie segun si ya estaba, igual que con las peliculas. */
    private fun alternarSerie(campo: String, serie: SerieModel, alTerminar: (Boolean) -> Unit) {
        val documento = documentoDelUsuario() ?: return alTerminar(false)
        leerSeries(campo) { series ->
            val yaEstaba = series.any { it.id == serie.id }
            val cambio = if (yaEstaba) FieldValue.arrayRemove(serie) else FieldValue.arrayUnion(serie)
            documento.set(mapOf(campo to cambio), SetOptions.merge())
                .addOnSuccessListener { alTerminar(true) }
                .addOnFailureListener { alTerminar(false) }
        }
    }

    /**
     * Las series guardadas, rehechas a mano igual que las peliculas.
     *
     * Ojo con los nombres: al guardar el objeto entero con arrayUnion, las claves que quedan
     * en Firestore son las de Kotlin (titulo, estreno, valoracion, poster), no las de TMDB.
     */
    private fun aSeries(bruto: Any?): List<SerieModel> {
        @Suppress("UNCHECKED_CAST")
        val lista = bruto as? List<Map<String, Any?>> ?: return emptyList()
        return lista.mapNotNull { mapa ->
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
    }

    private fun aEnteros(bruto: Any?): List<Int> {
        @Suppress("UNCHECKED_CAST")
        val lista = bruto as? List<Any?> ?: return emptyList()
        return lista.mapNotNull { (it as? Number)?.toInt() }
    }

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
        const val USUARIOS = "users"
        const val LISTAS = "lists"
        const val PELICULAS = "movies"
        const val FAVORITAS = "favorites"
        const val VISTAS = "watched"
        const val HISTORIAL = "history"
        const val SERIES = "series"
        const val SERIES_FAVORITAS = "favoriteSeries"
        const val SERIES_VISTAS = "watchedSeries"
        const val GUSTOS = "tastes"
        const val PELICULA_DEL_DIA = "dailymovie"
        const val CAMPO_FECHA = "date"
    }
}
