package com.example.dailymovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient

class LoginViewModel : ViewModel() {
    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    private val _resetPasswordStatus = MutableLiveData<Boolean>()
    val resetPasswordStatus: LiveData<Boolean> = _resetPasswordStatus

    fun loginUser(email: String, password: String) {
        FirebaseClient.loginUser(email, password) { success ->
            _loginStatus.value = success
        }
    }

    fun resetPassword(email: String) {
        FirebaseClient.resetPassword(email) { success ->
            _resetPasswordStatus.value = success
        }
    }

    fun getCurrentUser() = FirebaseClient.getCurrentUser()
}
