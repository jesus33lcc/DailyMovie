package com.example.dailymovie.client

import com.example.dailymovie.models.MovieModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
                    onComplete(false) // La película ya está en favoritos
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
                    onComplete(false) // La película no está en favoritos
                }
            } else {
                onComplete(false) // El documento no existe
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
                    onComplete(false) // La película ya está en vistos
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
                    onComplete(false) // La película no está en vistos
                }
            } else {
                onComplete(false) // El documento no existe
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

}
