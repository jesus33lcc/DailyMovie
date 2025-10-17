package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotoDePersona
import com.example.dailymovie.activities.viewmodels.PersonaViewModel
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.data.Filmografia
import com.example.dailymovie.databinding.ActivityPersonaBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.PersonModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

/**
 * Ficha de una persona del cine: vale igual para un actor que para un director.
 *
 * TMDB no distingue entre unos y otros, solo dice a que se dedica; asi que en vez de hacer
 * dos pantallas casi iguales, se hace una y se ocultan las secciones que no aplican. Si es
 * solo actor, no sale el apartado de peliculas dirigidas, y al contrario.
 */
class PersonaA : AppCompatActivity() {

    private lateinit var binding: ActivityPersonaBinding
    private val viewModel: PersonaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val personaId = intent.getIntExtra(EXTRA_PERSONA_ID, -1)
        if (personaId == -1) {
            Avisos.breve(binding.root, "No se ha podido abrir la ficha")
            finish()
            return
        }

        binding.recyclerActuando.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerActuando.addItemDecoration(SpacingItemDecoration.deLista(this))
        binding.recyclerDirigiendo.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerDirigiendo.addItemDecoration(SpacingItemDecoration.deLista(this))

        viewModel.ficha.observe(this) { pintarFicha(it) }
        viewModel.filmografia.observe(this) { pintarFilmografia(it) }
        viewModel.error.observe(this) { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
                viewModel.errorMostrado()
            }
        }

        viewModel.cargar(personaId)
    }

    private fun pintarFicha(persona: PersonModel) {
        binding.txtNombrePersona.text = persona.nombre
        binding.txtOficioPersona.text = traducirOficio(persona.oficio)
        binding.txtDatosPersona.text = datosDeVida(persona)

        binding.imgPersona.cargarFotoDePersona(persona.foto)

        // Muchas fichas no tienen biografia traducida al español, asi que se oculta en
        // vez de dejar un recuadro vacio.
        val hayBiografia = !persona.biografia.isNullOrBlank()
        binding.seccionBiografia.visibility = if (hayBiografia) View.VISIBLE else View.GONE
        binding.txtBiografia.text = persona.biografia
    }

    /** TMDB devuelve el oficio en ingles: Acting, Directing, Writing... */
    private fun traducirOficio(oficio: String?): String = when (oficio) {
        "Acting" -> "Actor / Actriz"
        "Directing" -> "Dirección"
        "Writing" -> "Guion"
        "Production" -> "Producción"
        "Sound" -> "Sonido"
        "Camera" -> "Fotografía"
        "Editing" -> "Montaje"
        null -> ""
        else -> oficio
    }

    /** Nacimiento, fallecimiento si lo hay, y de donde es. */
    private fun datosDeVida(persona: PersonModel): String {
        val trozos = mutableListOf<String>()
        persona.nacimiento?.takeIf { it.isNotBlank() }?.let { nacimiento ->
            val fallecimiento = persona.fallecimiento?.takeIf { it.isNotBlank() }
            trozos += if (fallecimiento == null) {
                Fechas.enLargo(nacimiento)
            } else {
                "${Fechas.enLargo(nacimiento)} — ${Fechas.enLargo(fallecimiento)}"
            }
        }
        persona.lugarDeNacimiento?.takeIf { it.isNotBlank() }?.let { trozos += it }
        return trozos.joinToString(" · ")
    }

    private fun pintarFilmografia(filmografia: Filmografia) {
        binding.seccionActuando.visibility =
            if (filmografia.actuando.isEmpty()) View.GONE else View.VISIBLE
        pintarFilmografia(binding.recyclerActuando, filmografia.actuando)

        binding.seccionDirigiendo.visibility =
            if (filmografia.dirigiendo.isEmpty()) View.GONE else View.VISIBLE
        pintarFilmografia(binding.recyclerDirigiendo, filmografia.dirigiendo)
    }

    /** La filmografia con la misma tarjeta que el resto del catalogo. */
    private fun pintarFilmografia(
        lista: androidx.recyclerview.widget.RecyclerView,
        peliculas: List<com.example.dailymovie.client.response.PeliculaDePersona>
    ) {
        val adaptador = lista.adapter as? HallazgoAdapter
            ?: HallazgoAdapter { hallazgo ->
                startActivity(Intent(this, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, hallazgo.id))
            }.also { lista.adapter = it }
        adaptador.submitList(peliculas.map { Hallazgo.de(it) })
    }

    companion object {
        const val EXTRA_PERSONA_ID = "PERSONA_ID"
    }
}
