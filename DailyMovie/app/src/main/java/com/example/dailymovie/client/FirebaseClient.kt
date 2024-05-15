package com.example.dailymovie.client

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

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
    fun addToFavorites(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val favorites = document.get("favorites") as? List<Int> ?: emptyList()
                if (movieId !in favorites) {
                    userDocRef.update("favorites", FieldValue.arrayUnion(movieId))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                userDocRef.set(mapOf("favorites" to listOf(movieId)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun removeFromFavorites(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        db.collection("users").document(userId)
            .update("favorites", FieldValue.arrayRemove(movieId))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun addToWatched(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val watched = document.get("watched") as? List<Int> ?: emptyList()
                if (movieId !in watched) {
                    userDocRef.update("watched", FieldValue.arrayUnion(movieId))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    onComplete(false)
                }
            } else {
                userDocRef.set(mapOf("watched" to listOf(movieId)))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun removeFromWatched(movieId: Int, onComplete: (Boolean) -> Unit) {
        val userId = getCurrentUserId() ?: return onComplete(false)
        db.collection("users").document(userId)
            .update("watched", FieldValue.arrayRemove(movieId))
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}