package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.activities.viewmodels.SagaViewModel
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.databinding.ActivitySagaBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.graphics.columnasDeCarteles
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.abrirFicha
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Fechas

/**
 * Las peliculas de una saga, en orden de estreno.
 *
 * TMDB llama colecciones a las sagas y no las devuelve en la busqueda normal, asi que hasta
 * ahora no habia forma de ver "El Señor de los Anillos" como un bloque: salian las tres
 * peliculas sueltas y mezcladas con las de la otra trilogia.
 *
 * Es una pantalla de solo lectura, sin nada de Firestore: se piden las peliculas una vez al
 * abrir y ya.
 */
class SagaA : AppCompatActivity() {

    private lateinit var binding: ActivitySagaBinding
    private val viewModel: SagaViewModel by viewModels()
    private lateinit var adaptador: HallazgoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySagaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtTituloSaga.text = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()

        adaptador = HallazgoAdapter { hallazgo, cartel -> abrirFicha(hallazgo, cartel) }
        binding.rvPeliculasDeLaSaga.layoutManager = GridLayoutManager(this, columnasDeCarteles())
        binding.rvPeliculasDeLaSaga.adapter = adaptador
        binding.rvPeliculasDeLaSaga.addItemDecoration(SpacingItemDecoration.deLista(this))

        viewModel.peliculasDeLaSaga.observe(this) { peliculas ->
            adaptador.submitList(peliculas)
            binding.txtCuantas.text = resumen(peliculas)
        }
        viewModel.cargando.observe(this) { cargando ->
            binding.progresoSaga.visibility = if (cargando) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
                viewModel.errorMostrado()
            }
        }

        viewModel.cargar(intent.getIntExtra(EXTRA_SAGA_ID, 0))
    }

    /** "3 películas · 1972 - 1990", que es lo que se quiere saber de un vistazo. */
    private fun resumen(peliculas: List<Hallazgo>): String {
        val cuantas = if (peliculas.size == 1) "1 película" else "${peliculas.size} películas"
        val anos = peliculas.mapNotNull { Fechas.soloElAno(it.subtitulo).toIntOrNull() }
        if (anos.isEmpty()) return cuantas
        val primero = anos.min()
        val ultimo = anos.max()
        return if (primero == ultimo) "$cuantas · $primero" else "$cuantas · $primero - $ultimo"
    }


    companion object {
        const val EXTRA_SAGA_ID = "SAGA_ID"
        const val EXTRA_NOMBRE = "SAGA_NOMBRE"
    }
}
