package com.example.dailymovie.client

import android.util.Log
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.MovieOfTheDay
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseClient {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    fun getCurrentUser() = auth.currentUser

    fun addToFavorites(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val favorites = document.get("favorites") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id !in favorites.map { (it["id"] as? Number)?.toInt() }) {
                    userDocRef.update("favorites", FieldValue.arrayUnion(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                userDocRef.set(mapOf("favorites" to listOf(movie)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun removeFromFavorites(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val favorites = document.get("favorites") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id in favorites.map { (it["id"] as? Number)?.toInt() }) {
                    userDocRef.update("favorites", FieldValue.arrayRemove(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun addToWatched(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val watched = document.get("watched") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id !in watched.map { (it["id"] as? Number)?.toInt() }) {
                    userDocRef.update("watched", FieldValue.arrayUnion(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                userDocRef.set(mapOf("watched" to listOf(movie)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun removeFromWatched(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val watched = document.get("watched") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id in watched.map { (it["id"] as? Number)?.toInt() }) {
                    userDocRef.update("watched", FieldValue.arrayRemove(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun getFavorites(onComplete: (List<MovieModel>) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(emptyList())
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val favorites = document.get("favorites") as? List<HashMap<String, Any>> ?: emptyList()
                    val movieList = favorites.mapNotNull { map ->
                        try {
                            val id = (map["id"] as? Number)?.toInt() ?: return@mapNotNull null
                            val title = map["title"] as? String ?: return@mapNotNull null
                            val releaseDate = map["releaseDate"] as? String ?: ""
                            val voteAverage = (map["voteAverage"] as? Number)?.toDouble() ?: 0.0
                            val posterPath = map["posterPath"] as? String

                            MovieModel(id, title, releaseDate, voteAverage, posterPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onComplete(movieList)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun getWatched(onComplete: (List<MovieModel>) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(emptyList())
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val watched = document.get("watched") as? List<HashMap<String, Any>> ?: emptyList()
                    val movieList = watched.mapNotNull { map ->
                        try {
                            val id = (map["id"] as? Number)?.toInt() ?: return@mapNotNull null
                            val title = map["title"] as? String ?: return@mapNotNull null
                            val releaseDate = map["releaseDate"] as? String ?: ""
                            val voteAverage = (map["voteAverage"] as? Number)?.toDouble() ?: 0.0
                            val posterPath = map["posterPath"] as? String

                            MovieModel(id, title, releaseDate, voteAverage, posterPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onComplete(movieList)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    fun getMovieOfTheDay(onComplete: (MovieOfTheDay?) -> Unit) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("dailymovie").get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val documentDate = document.getDate("date")
                    val formattedDocumentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(documentDate)
                    if (formattedDocumentDate == today) {
                        val id = document.getLong("id")?.toInt()
                        val personName = document.getString("person_name")
                        val review = document.getString("review")
                        val date = document.getDate("date")
                        val title = document.getString("title")
                        val videoId = document.getString("videoId")

                        if (id == null || personName == null || review == null || date == null || title == null || videoId == null) {
                            Log.e("FirebaseClient", "Faltan Datos")
                            onComplete(null)
                            return@addOnSuccessListener
                        }

                        onComplete(MovieOfTheDay(id, title, review, date.toString(), personName, videoId))
                        return@addOnSuccessListener
                    }
                }
                onComplete(null)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
    fun isMovieInFavorites(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val favorites = document.get("favorites") as? List<HashMap<String, Any>> ?: emptyList()
                val isFavorite = favorites.any { (it["id"] as? Number)?.toInt() == movieId }
                onComplete(isFavorite)
            } else {
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun isMovieInWatched(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val watched = document.get("watched") as? List<HashMap<String, Any>> ?: emptyList()
                val isWatched = watched.any { (it["id"] as? Number)?.toInt() == movieId }
                onComplete(isWatched)
            } else {
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun createNewList(listName: String, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userListsRef = db.collection("users").document(userId).collection("lists")

        userListsRef.document(listName).get().addOnSuccessListener { document ->
            if (document.exists()) {
                onComplete(false)
            } else {
                userListsRef.document(listName).set(emptyMap<String, Any>())
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun getCustomLists(onComplete: (List<String>) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(emptyList())
        db.collection("users").document(userId).collection("lists").get()
            .addOnSuccessListener { result ->
                val listNames = result.documents.map { it.id }
                onComplete(listNames)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    fun getMoviesFromList(listName: String, onComplete: (List<MovieModel>) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(emptyList())
        db.collection("users").document(userId).collection("lists").document(listName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val movies = document.get("movies") as? List<HashMap<String, Any>> ?: emptyList()
                    val movieList = movies.mapNotNull { map ->
                        try {
                            val id = (map["id"] as? Number)?.toInt() ?: return@mapNotNull null
                            val title = map["title"] as? String ?: return@mapNotNull null
                            val releaseDate = map["releaseDate"] as? String ?: ""
                            val voteAverage = (map["voteAverage"] as? Number)?.toDouble() ?: 0.0
                            val posterPath = map["posterPath"] as? String

                            MovieModel(id, title, releaseDate, voteAverage, posterPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onComplete(movieList)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    fun addMovieToList(listName: String, movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val listRef = db.collection("users").document(userId).collection("lists").document(listName)

        listRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val movies = document.get("movies") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id !in movies.map { (it["id"] as? Number)?.toInt() }) {
                    listRef.update("movies", FieldValue.arrayUnion(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                listRef.set(mapOf("movies" to listOf(movie)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
    fun removeMovieFromList(listName: String, movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val listDocRef = db.collection("users").document(userId).collection("lists").document(listName)

        listDocRef.update("movies", FieldValue.arrayRemove(movie))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
    fun deleteCustomList(listName: String, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val listRef = db.collection("users").document(userId).collection("lists").document(listName)

        listRef.delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
    fun createCustomList(listName: String, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val listRef = db.collection("users").document(userId).collection("lists").document(listName)

        listRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                onComplete(false)
            } else {
                listRef.set(mapOf("movies" to emptyList<MovieModel>()))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }
    }
    fun addToHistory(movie: MovieModel, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val history = document.get("history") as? List<HashMap<String, Any>> ?: emptyList()
                if (movie.id !in history.map { (it["id"] as? Number)?.toInt() }) {
                    userDocRef.update("history", FieldValue.arrayUnion(movie))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(true)
                }
            } else {
                userDocRef.set(mapOf("history" to listOf(movie)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun getHistory(onComplete: (List<MovieModel>) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(emptyList())
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val history = document.get("history") as? List<HashMap<String, Any>> ?: emptyList()
                    val movieList = history.mapNotNull { map ->
                        try {
                            val id = (map["id"] as? Number)?.toInt() ?: return@mapNotNull null
                            val title = map["title"] as? String ?: return@mapNotNull null
                            val releaseDate = map["releaseDate"] as? String ?: ""
                            val voteAverage = (map["voteAverage"] as? Number)?.toDouble() ?: 0.0
                            val posterPath = map["posterPath"] as? String

                            MovieModel(id, title, releaseDate, voteAverage, posterPath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onComplete(movieList)
                } else {
                    onComplete(emptyList())
                }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    fun clearHistory(onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.update("history", emptyList<MovieModel>())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
    fun logoutUser(onComplete: (Boolean) -> Unit) {
        try {
            auth.signOut()
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }
    fun changePassword(currentPassword: String, newPassword: String, onComplete: (Boolean, String) -> Unit) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            onComplete(true, "Contraseña actualizada exitosamente")
                        } else {
                            onComplete(false, updateTask.exception?.message ?: "Error desconocido")
                        }
                    }
                } else {
                    onComplete(false, reauthTask.exception?.message ?: "Error de autenticación")
                }
            }
        } else {
            onComplete(false, "Usuario no autenticado")
        }
    }
    fun registerUser(email: String, password: String, onComplete: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onComplete(false)
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
    fun loginUser(email: String, password: String, onComplete: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun resetPassword(email: String, onComplete: (Boolean) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

}
