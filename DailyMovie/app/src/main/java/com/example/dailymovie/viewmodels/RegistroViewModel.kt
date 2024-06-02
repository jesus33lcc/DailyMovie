package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient

class RegistroViewModel : ViewModel() {
    private val _registrationStatus = MutableLiveData<Boolean>()
    val registrationStatus: LiveData<Boolean> = _registrationStatus

    fun registerUser(email: String, password: String) {
        FirebaseClient.registerUser(email, password) { isSuccess ->
            _registrationStatus.value = isSuccess
        }
    }
}
