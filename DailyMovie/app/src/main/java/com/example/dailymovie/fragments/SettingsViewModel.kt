package com.example.dailymovie.fragments

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.dailymovie.client.FirebaseClient
import java.util.Locale

class SettingsViewModel : ViewModel() {
    fun clearHistory(onComplete: (Boolean) -> Unit) {
        FirebaseClient.clearHistory(onComplete)
    }
    fun logoutUser(onComplete: (Boolean) -> Unit) {
        FirebaseClient.logoutUser(onComplete)
    }
    fun changePassword(currentPassword: String, newPassword: String, onComplete: (Boolean, String) -> Unit) {
        FirebaseClient.changePassword(currentPassword, newPassword, onComplete)
    }
}