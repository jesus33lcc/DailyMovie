package com.example.dailymovie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dailymovie.activities.views.PrincipalA
import com.example.dailymovie.activities.views.RegistroA
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        val firebaseUser: FirebaseUser? = mAuth.currentUser

        if (firebaseUser != null) {
            val intent = Intent(applicationContext, PrincipalA::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, RegistroA::class.java)
            startActivity(intent)
            finish()
        }
    }
}