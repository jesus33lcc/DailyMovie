package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.dailymovie.adapters.ResenaAdapter
import com.example.dailymovie.adapters.ProviderAdapter
import com.example.dailymovie.adapters.TemporadaAdapter
import com.example.dailymovie.adapters.VideoAdapter
import com.example.dailymovie.client.response.SeasonResponse
import com.example.dailymovie.client.response.SerieDetailsResponse
import com.example.dailymovie.databinding.ActivitySerieBinding
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.utils.rebotar
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.LocaleUtil
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

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

        // El mismo nombre que el cartel de la tarjeta desde la que se ha llegado, para que
        // la imagen viaje hasta aqui en vez de aparecer de golpe.
        binding.imgPosterSerie.transitionName = "cartel_${TipoDeHallazgo.SERIE}_$serieId"
        if (serieId == -1) {
            Avisos.breve(binding.root, "No se ha podido abrir la serie")
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
        viewModel.episodiosVistos.observe(this) {
            temporadaEnPantalla?.let { temporada -> pintarEpisodios(temporada) }
            pintarProgresoDeLaSerie()
        }

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

        viewModel.clasificacionPorEdad.observe(this) { clasificacion ->
            // Si la serie no esta clasificada en este pais no se enseña nada: poner la de
            // otro pais confundiria mas de lo que aclara.
            binding.txtClasificacionSerie.visibility =
                if (clasificacion.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.txtClasificacionSerie.text = clasificacion.orEmpty()
        }

        viewModel.resenas.observe(this) { resenas ->
            binding.seccionResenasSerie.visibility =
                if (resenas.isEmpty()) View.GONE else View.VISIBLE
            if (resenas.isEmpty()) return@observe
            binding.recyclerResenasSerie.layoutManager = LinearLayoutManager(this)
            // Como mucho cinco, igual que en peliculas: con mas, la ficha se convierte en un foro.
            binding.recyclerResenasSerie.adapter = ResenaAdapter(resenas.take(5))
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
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
        totalDeEpisodios = serie.numeroDeEpisodios
        pintarProgresoDeLaSerie()

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

        // Una serie anunciada pero sin estrenar tiene fecha, asi que sin avisar parecia que
        // ya se podia empezar a ver.
        if (Fechas.estaPorVenir(serie.estreno)) {
            trozos += getString(R.string.proximamente, Fechas.enLargo(serie.estreno.orEmpty()))
        } else {
            val desde = Fechas.soloElAno(serie.estreno)
            val hasta = Fechas.soloElAno(serie.ultimaEmision)
            if (desde.isNotBlank()) {
                trozos += if (hasta.isBlank() || hasta == desde) desde else "$desde — $hasta"
            }
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

    /**
     * La temporada que se esta enseñando.
     *
     * Hace falta guardarla para poder repintar los episodios cuando cambie lo que el usuario
     * ha marcado como visto, sin volver a pedirla a TMDB.
     */
    private var temporadaEnPantalla: SeasonResponse? = null

    /** Cuantos episodios tiene la serie entera, para poder decir "llevas 12 de 62". */
    private var totalDeEpisodios = 0

    /**
     * Lo que llevas visto de la serie entera.
     *
     * Solo aparece cuando hay algo marcado: un "llevas 0 de 62" nada mas abrir la ficha
     * suena a reproche, no a informacion.
     */
    private fun pintarProgresoDeLaSerie() {
        val vistos = viewModel.episodiosVistos.value.orEmpty().size
        if (vistos == 0 || totalDeEpisodios <= 0) {
            binding.txtProgresoSerie.visibility = View.GONE
            return
        }
        binding.txtProgresoSerie.visibility = View.VISIBLE
        binding.txtProgresoSerie.text = if (vistos >= totalDeEpisodios) {
            "La has visto entera"
        } else {
            "Llevas $vistos de $totalDeEpisodios episodios"
        }
    }

    private fun pintarTemporada(temporada: SeasonResponse?) {
        // Mientras se piden los episodios se enseña el indicador, que en series largas
        // tarda lo suyo.
        if (temporada == null) {
            binding.progressEpisodios.visibility = View.VISIBLE
            binding.recyclerEpisodios.adapter = null
            temporadaEnPantalla = null
            return
        }

        binding.progressEpisodios.visibility = View.GONE
        temporadaEnPantalla = temporada
        pintarEpisodios(temporada)
    }

    /**
     * Los episodios de la temporada abierta, con lo que ya se ha visto marcado.
     *
     * Se vuelve a llamar cada vez que cambia el conjunto de vistos, asi la cabecera
     * ("3 de 13 vistos") y los ojos se quedan al dia sin volver a pedir nada a Firestore.
     */
    private fun pintarEpisodios(temporada: SeasonResponse) {
        val vistosDeEstaTemporada = viewModel.episodiosVistos.value.orEmpty()
            .filter { it.first == temporada.numero }
            .map { it.second }
            .toSet()

        binding.txtTemporadaAbierta.text = if (vistosDeEstaTemporada.isEmpty()) {
            "${temporada.nombre} · ${temporada.episodios.size} episodios"
        } else {
            "${temporada.nombre} · ${vistosDeEstaTemporada.size} de ${temporada.episodios.size} vistos"
        }

        binding.recyclerEpisodios.adapter = EpisodioAdapter(
            episodios = temporada.episodios,
            vistos = vistosDeEstaTemporada,
            // Sin sesion no hay donde guardarlo, asi que el adaptador esconde el boton.
            alMarcar = if (viewModel.haySesion()) {
                { numero -> viewModel.cambiarEpisodioVisto(temporada.numero, numero) }
            } else {
                null
            }
        )
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
            viewModel.cambiarFavorita(guardable) { pintarIconoFavorita(guardable.id, conRebote = true) }
        }
        binding.btnVistaSerie.setOnClickListener {
            viewModel.cambiarVista(guardable) { pintarIconoVista(guardable.id, conRebote = true) }
        }
        binding.btnAnadirListaSerie.setOnClickListener { elegirListas(guardable) }
        binding.btnCompartirSerie.setOnClickListener { compartir(guardable) }
    }

    /**
     * @param conRebote si el icono cambia porque el usuario acaba de pulsar. Al abrir la
     *   ficha tambien se pinta, y ahi un boton dando saltos solo distrae.
     */
    private fun pintarIconoFavorita(serieId: Int, conRebote: Boolean = false) {
        viewModel.esFavorita(serieId) { esFavorita ->
            binding.btnFavoritaSerie.setImageResource(
                if (esFavorita) R.drawable.ic_baseline_favorite_24
                else R.drawable.ic_baseline_favorite_border_24
            )
            if (conRebote) binding.btnFavoritaSerie.rebotar()
        }
    }

    /** @param conRebote lo mismo que en [pintarIconoFavorita]. */
    private fun pintarIconoVista(serieId: Int, conRebote: Boolean = false) {
        viewModel.estaVista(serieId) { estaVista ->
            binding.btnVistaSerie.setImageResource(
                if (estaVista) R.drawable.ic_baseline_visibility_24
                else R.drawable.ic_baseline_visibility_off_24
            )
            if (conRebote) binding.btnVistaSerie.rebotar()
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
        Avisos.breve(binding.root, aviso)
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
