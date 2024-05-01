package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel (){
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginStatus.value = false
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loginStatus.value = task.isSuccessful
            }
            .addOnFailureListener {
                _loginStatus.value = false
            }
    }
}