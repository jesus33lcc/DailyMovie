package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dailymovie.activities.viewmodels.OnboardingViewModel
import com.example.dailymovie.adapters.Elegible
import com.example.dailymovie.adapters.ElegibleAdapter
import com.example.dailymovie.databinding.ActivityOnboardingBinding

/**
 * Las preguntas que se le hacen al usuario nada mas registrarse.
 *
 * Son tres pasos en una sola pantalla que va cambiando de contenido, en vez de tres
 * Activities: asi el usuario puede ir y volver sin perder lo que llevaba marcado.
 *
 * Todo es opcional. Si pasa de contestar, la portada tira de lo popular y siempre puede
 * volver a esto desde Ajustes.
 */
class OnboardingA : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private var paso = PASO_GENEROS

    /** Si se entra desde Ajustes, al terminar se vuelve atras en vez de ir a la portada. */
    private val vengoDeAjustes by lazy { intent.getBooleanExtra(EXTRA_DESDE_AJUSTES, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // En una tablet caben mas fichas por fila; con dos quedaria medio vacio.
        binding.onboardingRecycler.layoutManager = GridLayoutManager(this, columnasSegunPantalla())

        binding.onboardingSiguiente.setOnClickListener { avanzar() }
        binding.onboardingSaltar.setOnClickListener { terminar() }
        prepararBotonAtras()

        viewModel.cargando.observe(this) { cargando ->
            binding.onboardingProgress.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        viewModel.generos.observe(this) { generos ->
            if (paso != PASO_GENEROS) return@observe
            pintar(generos.map { Elegible(it.id, it.name, null) },
                viewModel::generoEstaElegido, viewModel::marcarGenero)
        }

        viewModel.peliculasSugeridas.observe(this) { peliculas ->
            if (paso == PASO_GENEROS) return@observe
            pintar(peliculas.map { Elegible(it.id, it.title, it.posterPath) },
                viewModel::peliculaEstaElegida, viewModel::marcarPelicula)
        }

        viewModel.guardado.observe(this) { guardado ->
            if (guardado == null) return@observe
            if (!guardado) Toast.makeText(this, "No se han podido guardar tus gustos", Toast.LENGTH_SHORT).show()
            salir()
        }

        if (vengoDeAjustes) {
            viewModel.cargarGustosGuardados { mostrarPaso() }
        } else {
            mostrarPaso()
        }
    }

    private fun mostrarPaso() {
        when (paso) {
            PASO_GENEROS -> {
                binding.onboardingPaso.text = "Paso 1 de 2"
                binding.onboardingTitulo.text = "¿Qué géneros te gustan?"
                binding.onboardingExplicacion.text =
                    "Elige los que más veas. Con esto elegimos la película que te " +
                        "enseñamos al abrir la app."
                binding.onboardingSiguiente.text = "Siguiente"
                viewModel.cargarGeneros()
            }
            PASO_PELICULAS -> {
                binding.onboardingPaso.text = "Paso 2 de 2"
                binding.onboardingTitulo.text = "¿Alguna de estas te suena?"
                binding.onboardingExplicacion.text =
                    "Marca las que ya has visto y te gustaron. Así no te las volvemos a " +
                        "recomendar y afinamos con las demás."
                binding.onboardingSiguiente.text = "Terminar"
                viewModel.cargarPeliculasSugeridas()
            }
        }
    }

    private fun avanzar() {
        when (paso) {
            PASO_GENEROS -> {
                if (!viewModel.hayAlgunGenero()) {
                    Toast.makeText(this, "Elige al menos un género, o toca \"Ahora no\"", Toast.LENGTH_SHORT).show()
                    return
                }
                paso = PASO_PELICULAS
                mostrarPaso()
            }
            PASO_PELICULAS -> terminar()
        }
    }

    private fun terminar() {
        viewModel.guardar()
    }

    private fun salir() {
        if (vengoDeAjustes) {
            finish()
        } else {
            startActivity(Intent(this, PrincipalA::class.java))
            finish()
        }
    }

    private fun pintar(
        elementos: List<Elegible>,
        estaElegido: (Int) -> Boolean,
        alTocar: (Int, Boolean) -> Unit
    ) {
        binding.onboardingRecycler.adapter = ElegibleAdapter(elementos, estaElegido, alTocar)
    }

    /**
     * Con el boton de atras no se pierde lo marcado: se guarda y se sale.
     *
     * Se usa el dispatcher y no el onBackPressed de toda la vida, que esta obsoleto desde
     * Android 13 y ademas obliga a llamar al super, que aqui no interesa porque queremos
     * decidir a donde va el usuario.
     */
    private fun prepararBotonAtras() {
        onBackPressedDispatcher.addCallback(this) { terminar() }
    }

    /** Una columna por cada 180dp de ancho, con un minimo de dos. */
    private fun columnasSegunPantalla(): Int {
        val anchoEnDp = resources.configuration.screenWidthDp
        return (anchoEnDp / 180).coerceAtLeast(2)
    }

    companion object {
        const val EXTRA_DESDE_AJUSTES = "DESDE_AJUSTES"
        private const val PASO_GENEROS = 0
        private const val PASO_PELICULAS = 1
    }
}
