package com.example.dailymovie.utils

import android.app.Activity
import android.content.Intent
import android.view.View
import com.example.dailymovie.activities.views.MovieA
import com.example.dailymovie.activities.views.PersonaA
import com.example.dailymovie.activities.views.SagaA
import com.example.dailymovie.activities.views.SerieA
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo

/**
 * Abre la ficha que le toca a un resultado de busqueda.
 *
 * Este `when` estaba repetido en seis pantallas (portada, series, explorar, persona, la propia
 * ficha de pelicula y la de una lista) y cada copia se habia quedado en un punto distinto: la
 * de Explorar era la unica que sabia abrir sagas, y las demas mandaban las sagas a la ficha de
 * pelicula, donde el id de una coleccion no existe y salia una pantalla rota.
 *
 * @param hallazgo lo que se ha tocado.
 * @param cartel la imagen de la tarjeta, si la hay. Con ella el cartel viaja hasta la ficha en
 *   vez de que la pantalla nueva aparezca de golpe; sin ella se abre normal.
 */
fun Activity.abrirFicha(hallazgo: Hallazgo, cartel: View? = null) {
    val destino = when (hallazgo.tipo) {
        TipoDeHallazgo.PELICULA ->
            Intent(this, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, hallazgo.id)
        TipoDeHallazgo.SERIE ->
            Intent(this, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, hallazgo.id)
        TipoDeHallazgo.PERSONA ->
            Intent(this, PersonaA::class.java).putExtra(PersonaA.EXTRA_PERSONA_ID, hallazgo.id)
        TipoDeHallazgo.SAGA ->
            Intent(this, SagaA::class.java)
                .putExtra(SagaA.EXTRA_SAGA_ID, hallazgo.id)
                .putExtra(SagaA.EXTRA_NOMBRE, hallazgo.titulo)
    }

    if (cartel == null) {
        startActivity(destino)
    } else {
        abrirConCartel(destino, cartel, HallazgoAdapter.nombreDeTransicion(hallazgo))
    }
}
