package com.example.dailymovie.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.adapters.ListaCasillaAdapter

/**
 * El diálogo de "guardar en una lista", que es el mismo para películas y para series.
 *
 * Estaban las dos versiones escritas enteras, unas ochenta líneas calcadas en `MovieA` y en
 * `SerieA`, y lo único que cambiaba era el tipo de lo que se guarda y el título que se enseña
 * arriba. Cualquier arreglo —como la guarda de "el usuario ya se ha ido"— había que hacerlo
 * dos veces o se quedaba a medias en una de las dos.
 *
 * Los datos entran por parámetro en vez de llamar al repositorio desde aquí: así esto sigue
 * siendo solo pantalla, y cada ficha pide sus listas a su propio ViewModel.
 *
 * @param titulo lo que se enseña debajo del encabezado: el nombre de la película o la serie.
 * @param listasDelUsuario las que tiene creadas. Si no tiene ninguna se explica cómo crearlas
 *   en vez de abrir un diálogo vacío.
 * @param yaGuardadaEn en cuáles está ya, para abrir las casillas marcadas.
 * @param alAplicar recibe en qué listas hay que meterla y de cuáles sacarla, ya calculado. Solo
 *   se llama si algo ha cambiado de verdad.
 */
fun AppCompatActivity.elegirListas(
    titulo: String,
    listasDelUsuario: List<String>,
    yaGuardadaEn: Set<String>,
    alAplicar: (meter: Set<String>, sacar: Set<String>) -> Unit
) {
    // Dos saltos a Firestore antes de llegar aquí: si el usuario se ha ido mientras tanto, el
    // contexto ya no vale y abrir la ventana revienta.
    if (isFinishing || isDestroyed) return

    if (listasDelUsuario.isEmpty()) {
        DialogoDailyMovie.mostrar(
            context = this,
            titulo = "Guardar en una lista",
            mensaje = "Todavía no tienes ninguna lista tuya. Créala en la pestaña " +
                "Listas y aquí podrás guardar lo que quieras.",
            textoAceptar = "Entendido",
            textoCancelar = null
        ) { it.dismiss() }
        return
    }

    val marcadas = yaGuardadaEn.toMutableSet()
    val contenido = layoutInflater.inflate(R.layout.dialogo_listas, null)
    contenido.findViewById<RecyclerView>(R.id.dialogoListas).apply {
        layoutManager = LinearLayoutManager(this@elegirListas)
        adapter = ListaCasillaAdapter(listasDelUsuario, marcadas)
    }

    DialogoDailyMovie.mostrar(
        context = this,
        titulo = "Guardar en una lista",
        mensaje = titulo,
        contenido = contenido,
        textoAceptar = "Guardar"
    ) { dialogo ->
        dialogo.dismiss()
        val meter = marcadas - yaGuardadaEn
        val sacar = yaGuardadaEn - marcadas
        // Sin cambios no se toca nada ni se avisa: cerrar el diálogo sin haber marcado nada
        // no es guardar.
        if (meter.isNotEmpty() || sacar.isNotEmpty()) alAplicar(meter, sacar)
    }
}

/**
 * El aviso de después de guardar, que cuenta lo que ha pasado.
 *
 * @param meter en qué listas ha entrado.
 * @param sacar de cuáles ha salido.
 * @return el texto para el aviso.
 */
fun avisoDeListas(meter: Set<String>, sacar: Set<String>): String = when {
    meter.isNotEmpty() && sacar.isEmpty() -> "Guardada en ${meter.joinToString(", ")}"
    meter.isEmpty() && sacar.isNotEmpty() -> "Quitada de ${sacar.joinToString(", ")}"
    else -> "Listas actualizadas"
}
