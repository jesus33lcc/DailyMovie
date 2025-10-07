package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.activities.viewmodels.SerieViewModel
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.adapters.CreditAdapter
import com.example.dailymovie.adapters.ListaCasillaAdapter
import com.example.dailymovie.adapters.EpisodioAdapter
import com.example.dailymovie.adapters.ProviderAdapter
import com.example.dailymovie.adapters.TemporadaAdapter
import com.example.dailymovie.adapters.VideoAdapter
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.databinding.ActivitySerieBinding
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.LocaleUtil
import com.example.dailymovie.utils.mensaje

/**
 * Ficha de una serie.
 *
 * Se parece a la de pelicula, pero con lo suyo: temporadas y episodios. Los episodios no se
 * piden todos de golpe; solo los de la temporada que el usuario tiene abierta, que en una
 * serie larga son bastantes menos datos.
 */
class SerieA : AppCompatActivity() {

    private lateinit var binding: ActivitySerieBinding
    private val viewModel: SerieViewModel by viewModels()

    /** La serie en su version corta, que es la que se guarda en las listas. */
    private var serieGuardable: SerieModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySerieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serieId = intent.getIntExtra(EXTRA_SERIE_ID, -1)
        if (serieId == -1) {
            Toast.makeText(this, "No se ha podido abrir la serie", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        prepararListas()
        observarViewModel()
        viewModel.cargar(serieId)
    }

    private fun prepararListas() {
        binding.recyclerTemporadas.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerEpisodios.layoutManager = LinearLayoutManager(this)
        binding.recyclerRepartoSerie.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerRepartoSerie.addItemDecoration(SpacingItemDecoration.deLista(this))
        binding.recyclerPlataformasSerie.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerVideosSerie.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerVideosSerie.addItemDecoration(SpacingItemDecoration.deLista(this))
        binding.recyclerPlataformasSerie.addItemDecoration(SpacingItemDecoration.deLista(this))
    }

    private fun observarViewModel() {
        viewModel.detalles.observe(this) { pintarSerie(it) }
        viewModel.temporadaAbierta.observe(this) { pintarTemporada(it) }

        viewModel.reparto.observe(this) { creditos ->
            val hayReparto = creditos.cast.isNotEmpty()
            binding.seccionRepartoSerie.visibility = if (hayReparto) View.VISIBLE else View.GONE
            binding.recyclerRepartoSerie.adapter = CreditAdapter(creditos.cast)
        }

        viewModel.videos.observe(this) { videos ->
            binding.seccionVideosSerie.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
            binding.recyclerVideosSerie.adapter = VideoAdapter(videos)
        }

        viewModel.plataformas.observe(this) { respuesta ->
            val plataformas = respuesta.results[LocaleUtil.getDeviceCountry()]?.flatrate.orEmpty()
            binding.seccionPlataformasSerie.visibility =
                if (plataformas.isEmpty()) View.GONE else View.VISIBLE
            binding.recyclerPlataformasSerie.adapter = ProviderAdapter(plataformas)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, getString(it.mensaje()), Toast.LENGTH_SHORT).show()
                viewModel.errorMostrado()
            }
        }
    }

    private fun pintarSerie(serie: SerieDetailsResponse) {
        binding.txtTituloSerie.text = serie.titulo
        binding.txtSinopsisSerie.text = serie.sinopsis.orEmpty()
        binding.txtSinopsisSerie.visibility =
            if (serie.sinopsis.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.txtGenerosSerie.text = serie.generos.joinToString { it.name }
        binding.txtDatosSerie.text = resumenDeLaSerie(serie)

        val estado = traducirEstado(serie.estado)
        binding.txtEstadoSerie.visibility = if (estado.isBlank()) View.GONE else View.VISIBLE
        binding.txtEstadoSerie.text = estado

        binding.imgPosterSerie.cargarCartel(serie.poster)

        prepararBotonesDeGuardado(serie)

        // La temporada 0 son los especiales; no es por donde se empieza una serie, asi que
        // no aparece entre los botones.
        val temporadas = serie.temporadas.filter { it.numero > 0 }
        binding.seccionTemporadas.visibility = if (temporadas.isEmpty()) View.GONE else View.VISIBLE
        binding.recyclerTemporadas.adapter = TemporadaAdapter(temporadas) { elegida ->
            viewModel.abrirTemporada(elegida.numero)
        }
    }

    /** "2008 — 2013 · 5 temporadas · 62 episodios" */
    private fun resumenDeLaSerie(serie: SerieDetailsResponse): String {
        val trozos = mutableListOf<String>()

        val desde = Fechas.soloElAno(serie.estreno)
        val hasta = Fechas.soloElAno(serie.ultimaEmision)
        if (desde.isNotBlank()) {
            trozos += if (hasta.isBlank() || hasta == desde) desde else "$desde — $hasta"
        }
        if (serie.numeroDeTemporadas > 0) {
            trozos += if (serie.numeroDeTemporadas == 1) "1 temporada"
            else "${serie.numeroDeTemporadas} temporadas"
        }
        if (serie.numeroDeEpisodios > 0) trozos += "${serie.numeroDeEpisodios} episodios"

        return trozos.joinToString(" · ")
    }

    private fun traducirEstado(estado: String?): String = when (estado) {
        "Ended" -> "Terminada"
        "Returning Series" -> "En emisión"
        "Canceled" -> "Cancelada"
        "In Production" -> "En producción"
        "Planned" -> "Anunciada"
        "Pilot" -> "Piloto"
        else -> ""
    }

    private fun pintarTemporada(temporada: SeasonResponse?) {
        // Mientras se piden los episodios se enseña el indicador, que en series largas
        // tarda lo suyo.
        if (temporada == null) {
            binding.progressEpisodios.visibility = View.VISIBLE
            binding.recyclerEpisodios.adapter = null
            return
        }

        binding.progressEpisodios.visibility = View.GONE
        binding.txtTemporadaAbierta.text =
            "${temporada.nombre} · ${temporada.episodios.size} episodios"
        binding.recyclerEpisodios.adapter = EpisodioAdapter(temporada.episodios)
    }

    /**
     * Los botones de guardar, los mismos que en la ficha de pelicula.
     *
     * La serie se queda en su version corta (SerieModel) porque es lo que se guarda en
     * Firestore: la ficha completa trae temporadas y episodios, que no pintan nada dentro
     * de una lista.
     */
    private fun prepararBotonesDeGuardado(serie: SerieDetailsResponse) {
        val guardable = SerieModel(
            id = serie.id,
            titulo = serie.titulo,
            estreno = serie.estreno,
            valoracion = serie.valoracion,
            poster = serie.poster
        )
        serieGuardable = guardable

        pintarIconoFavorita(guardable.id)
        pintarIconoVista(guardable.id)

        binding.btnFavoritaSerie.setOnClickListener {
            viewModel.cambiarFavorita(guardable) { pintarIconoFavorita(guardable.id) }
        }
        binding.btnVistaSerie.setOnClickListener {
            viewModel.cambiarVista(guardable) { pintarIconoVista(guardable.id) }
        }
        binding.btnAnadirListaSerie.setOnClickListener { elegirListas(guardable) }
        binding.btnCompartirSerie.setOnClickListener { compartir(guardable) }
    }

    private fun pintarIconoFavorita(serieId: Int) {
        viewModel.esFavorita(serieId) { esFavorita ->
            binding.btnFavoritaSerie.setImageResource(
                if (esFavorita) R.drawable.ic_baseline_favorite_24
                else R.drawable.ic_baseline_favorite_border_24
            )
        }
    }

    private fun pintarIconoVista(serieId: Int) {
        viewModel.estaVista(serieId) { estaVista ->
            binding.btnVistaSerie.setImageResource(
                if (estaVista) R.drawable.ic_baseline_visibility_24
                else R.drawable.ic_baseline_visibility_off_24
            )
        }
    }

    /** El mismo dialogo de casillas que en peliculas, pero con las listas de la serie. */
    private fun elegirListas(serie: SerieModel) {
        viewModel.listasDelUsuario { nombres ->
            if (nombres.isEmpty()) {
                DialogoDailyMovie.mostrar(
                    context = this,
                    titulo = "Guardar en una lista",
                    mensaje = "Todavía no tienes ninguna lista tuya. Créala en la pestaña " +
                        "Listas y aquí podrás guardar lo que quieras.",
                    textoAceptar = "Entendido",
                    textoCancelar = null
                ) { it.dismiss() }
                return@listasDelUsuario
            }

            viewModel.listasConLaSerie(serie.id) { yaEstaba ->
                val marcadas = yaEstaba.toMutableSet()
                val contenido = layoutInflater.inflate(R.layout.dialogo_listas, null)
                val lista = contenido.findViewById<RecyclerView>(R.id.dialogoListas)
                lista.layoutManager = LinearLayoutManager(this)
                lista.adapter = ListaCasillaAdapter(nombres, marcadas)

                DialogoDailyMovie.mostrar(
                    context = this,
                    titulo = "Guardar en una lista",
                    mensaje = serie.titulo,
                    contenido = contenido,
                    textoAceptar = "Guardar"
                ) { dialogo ->
                    dialogo.dismiss()
                    aplicarCambiosDeListas(serie, yaEstaba, marcadas)
                }
            }
        }
    }

    /** Solo se tocan las listas que han cambiado. */
    private fun aplicarCambiosDeListas(serie: SerieModel, antes: Set<String>, ahora: Set<String>) {
        val meter = ahora - antes
        val sacar = antes - ahora
        if (meter.isEmpty() && sacar.isEmpty()) return

        meter.forEach { viewModel.anadirALista(it, serie) { } }
        sacar.forEach { viewModel.quitarDeLista(it, serie) { } }

        val aviso = when {
            meter.isNotEmpty() && sacar.isEmpty() -> "Guardada en ${meter.joinToString(", ")}"
            meter.isEmpty() && sacar.isNotEmpty() -> "Quitada de ${sacar.joinToString(", ")}"
            else -> "Listas actualizadas"
        }
        Toast.makeText(this, aviso, Toast.LENGTH_SHORT).show()
    }

    private fun compartir(serie: SerieModel) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Te recomiendo esta serie: ${serie.titulo}\n${Constantes.BASE_SERIE_URL}${serie.id}"
            )
        }
        startActivity(Intent.createChooser(intent, "Compartir serie"))
    }

    companion object {
        const val EXTRA_SERIE_ID = "SERIE_ID"
    }
}
