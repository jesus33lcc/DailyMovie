package com.example.dailymovie

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dailymovie.activities.views.PrincipalA
import com.example.dailymovie.activities.views.RegistroA
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.utils.Analitica

/**
 * La primera pantalla: solo mira si hay sesion y manda a donde toque.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Se respeta lo que el usuario eligio sobre los datos de uso. Si nunca ha dicho
        // nada, la analitica arranca apagada.
        Analitica.aplicarLoGuardado(this)

        val destino = if (Dependencias.usuario.haySesion()) {
            PrincipalA::class.java
        } else {
            RegistroA::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}