package com.example.dailymovie.fragments.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.R
import com.example.dailymovie.activities.views.MovieA
import com.example.dailymovie.activities.views.PersonaA
import com.example.dailymovie.activities.views.SerieA
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.databinding.FragmentExplorarBinding
import com.example.dailymovie.fragments.viewmodels.ExplorarViewModel
import com.example.dailymovie.fragments.viewmodels.OrdenDeBusqueda
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.GeneroExplorable
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.utils.BusquedasRecientes
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

/**
 * Explorar.
 *
 * La pantalla tiene tres caras y solo se ve una: lo que hay para descubrir cuando no has
 * escrito nada, los resultados de la busqueda, o el aviso de que no se ha encontrado nada.
 * Quien manda es si hay o no una busqueda en marcha.
 */
class ExplorarF : Fragment() {

    private val explorarViewModel: ExplorarViewModel by viewModels()
    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!

    private lateinit var resultadosAdapter: HallazgoAdapter

    /**
     * Para poder escribir en el campo sin que salga a buscar.
     *
     * Al tocar un genero se pone su nombre en la barra para que se vea que estas viendo, pero
     * la busqueda ya la ha lanzado explorarGenero. Sin esta bandera, el setText disparaba el
     * oyente del texto y se buscaba "Accion" por titulo, que devuelve "Maridos en accion" y
     * "Granjas en accion" en vez de peliculas de accion.
     */
    private var textoPuestoPorCodigo = false

    /**
     * Para volver arriba cuando cambia el orden o el filtro.
     *
     * Al reordenar, el RecyclerView intenta no perder de vista lo que estabas mirando y
     * sigue a esa tarjeta a su sitio nuevo: si ordenabas por nota y "La odisea" se iba al
     * puesto quince, la pantalla bajaba hasta alli. Lo que uno espera al ordenar es ver lo
     * primero de la lista nueva.
     */
    private var volverArriba = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExplorarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prepararResultados()
        prepararBuscador()
        prepararFiltros()
        observarViewModel()

        binding.btnSorprendeme.setOnClickListener { sorprender() }
        binding.btnOlvidarRecientes.setOnClickListener {
            BusquedasRecientes.olvidarTodas(requireContext())
            pintarRecientes()
        }

        explorarViewModel.cargarTendencias()
        explorarViewModel.cargarGeneros()
    }

    override fun onResume() {
        super.onResume()
        explorarViewModel.loadHistory()
        pintarRecientes()
    }

    // ---------------- Buscar ----------------

    private fun prepararBuscador() {
        // El freno esta en el ViewModel: aqui solo se le dice lo que hay escrito y el decide
        // cuando salir a la red.
        binding.searchInput.doAfterTextChanged { texto ->
            val consulta = texto?.toString().orEmpty()
            binding.clearSearchIcon.visibility =
                if (consulta.isEmpty()) View.GONE else View.VISIBLE

            if (textoPuestoPorCodigo) {
                textoPuestoPorCodigo = false
                return@doAfterTextChanged
            }

            explorarViewModel.buscar(consulta)
            decidirQueSeVe()
        }

        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            val consulta = binding.searchInput.text.toString().trim()
            if (consulta.isNotEmpty()) {
                // Solo se guarda lo que se busca a proposito, no cada letra que se teclea.
                BusquedasRecientes.guardar(requireContext(), consulta)
                esconderTeclado()
            }
            true
        }

        binding.clearSearchIcon.setOnClickListener {
            binding.searchInput.text.clear()
            esconderTeclado()
        }
    }

    private fun prepararResultados() {
        resultadosAdapter = HallazgoAdapter { hallazgo -> abrir(hallazgo) }
        binding.rvListaBusqueda.layoutManager = GridLayoutManager(context, columnasQueCaben())
        binding.rvListaBusqueda.adapter = resultadosAdapter
        binding.rvListaBusqueda.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
    }

    /** Las mismas columnas que en las listas: lo que quepa segun el ancho que haya. */
    private fun columnasQueCaben(): Int {
        val ancho = resources.displayMetrics.widthPixels
        val cartel = resources.getDimensionPixelSize(R.dimen.poster_ancho)
        val hueco = resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)
        return ((ancho - hueco * 4) / (cartel + hueco * 2)).coerceAtLeast(2)
    }

    private fun prepararFiltros() {
        listOf(
            binding.chipTodo to null,
            binding.chipPeliculas to TipoDeHallazgo.PELICULA,
            binding.chipSeries to TipoDeHallazgo.SERIE,
            binding.chipPersonas to TipoDeHallazgo.PERSONA
        ).forEach { (chip, tipo) ->
            chip.setOnClickListener {
                volverArriba = true
                explorarViewModel.cambiarFiltro(explorarViewModel.filtro.copy(tipo = tipo))
                pintarChips()
            }
        }

        binding.btnOrdenar.setOnClickListener { boton ->
            PopupMenu(ContextThemeWrapper(requireContext(), R.style.TemaPopupDailyMovie), boton).apply {
                inflate(R.menu.menu_orden_busqueda)
                setOnMenuItemClickListener { opcion ->
                    val orden = when (opcion.itemId) {
                        R.id.orden_nota -> OrdenDeBusqueda.NOTA
                        R.id.orden_ano -> OrdenDeBusqueda.ANO
                        else -> OrdenDeBusqueda.RELEVANCIA
                    }
                    volverArriba = true
                    explorarViewModel.cambiarFiltro(explorarViewModel.filtro.copy(orden = orden))
                    true
                }
                show()
            }
        }
    }

    private fun pintarChips() {
        val filtro = explorarViewModel.filtro.tipo
        listOf(
            Triple(binding.chipTodo, null as TipoDeHallazgo?, "Todo"),
            Triple(binding.chipPeliculas, TipoDeHallazgo.PELICULA, "Películas"),
            Triple(binding.chipSeries, TipoDeHallazgo.SERIE, "Series"),
            Triple(binding.chipPersonas, TipoDeHallazgo.PERSONA, "Gente")
        ).forEach { (chip, tipo, nombre) ->
            val cuantos = explorarViewModel.cuantosHayDe(tipo)
            chip.text = if (cuantos > 0) "$nombre · $cuantos" else nombre
            chip.isSelected = filtro == tipo
            // Un chip de un tipo del que no hay nada solo estorba.
            chip.visibility = if (tipo == null || cuantos > 0) View.VISIBLE else View.GONE
        }
    }

    // ---------------- Lo que se ve en cada momento ----------------

    private fun observarViewModel() {
        explorarViewModel.resultados.observe(viewLifecycleOwner) { hallazgos ->
            // El subir se hace en el callback, cuando la lista nueva ya esta pintada: antes
            // de eso el RecyclerView todavia tiene las posiciones viejas.
            resultadosAdapter.submitList(hallazgos) {
                if (volverArriba) {
                    volverArriba = false
                    binding.rvListaBusqueda.scrollToPosition(0)
                }
            }
            pintarChips()
            decidirQueSeVe()
        }

        explorarViewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            binding.progresoBusqueda.visibility = if (cargando) View.VISIBLE else View.GONE
        }

        explorarViewModel.sinResultados.observe(viewLifecycleOwner) { decidirQueSeVe() }

        explorarViewModel.tendencias.observe(viewLifecycleOwner) { tendencias ->
            binding.seccionTendencias.visibility =
                if (tendencias.isEmpty()) View.GONE else View.VISIBLE
            binding.rvTendencias.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            if (binding.rvTendencias.itemDecorationCount == 0) {
                binding.rvTendencias.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
            }
            val adaptador = HallazgoAdapter { abrir(it) }
            binding.rvTendencias.adapter = adaptador
            adaptador.submitList(tendencias)
        }

        explorarViewModel.generos.observe(viewLifecycleOwner) { pintarGeneros(it) }

        explorarViewModel.history.observe(viewLifecycleOwner) { historial ->
            binding.seccionHistorial.visibility =
                if (historial.isEmpty()) View.GONE else View.VISIBLE
            binding.rvHistorial.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            if (binding.rvHistorial.itemDecorationCount == 0) {
                binding.rvHistorial.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
            }
            val adaptadorHistorial = HallazgoAdapter { abrir(it) }
            binding.rvHistorial.adapter = adaptadorHistorial
            adaptadorHistorial.submitList(historial.map { Hallazgo.de(it) })
        }

        explorarViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
                explorarViewModel.errorMostrado()
            }
        }
    }

    /**
     * Decide cual de las tres caras toca.
     *
     * Se llama desde varios sitios (al teclear, al llegar resultados, al cambiar el filtro)
     * porque cualquiera de esas cosas cambia lo que hay que enseñar.
     */
    private fun decidirQueSeVe() {
        val buscando = explorarViewModel.hayBusquedaEnMarcha()
        val hayResultados = !explorarViewModel.resultados.value.isNullOrEmpty()
        val nadaEncontrado = explorarViewModel.sinResultados.value == true

        binding.panelExplorar.visibility = if (buscando) View.GONE else View.VISIBLE
        binding.barraFiltros.visibility = if (buscando && hayResultados) View.VISIBLE else View.GONE
        binding.rvListaBusqueda.visibility = if (buscando && hayResultados) View.VISIBLE else View.GONE

        val sinNada = buscando && nadaEncontrado && !hayResultados
        binding.panelSinNada.visibility = if (sinNada) View.VISIBLE else View.GONE
        if (sinNada) {
            binding.txtSinNada.text = getString(
                R.string.busqueda_sin_resultados,
                binding.searchInput.text.toString().trim()
            )
        }
    }

    // ---------------- Lo de descubrir ----------------

    private fun pintarRecientes() {
        val recientes = BusquedasRecientes.todas(requireContext())
        binding.seccionRecientes.visibility = if (recientes.isEmpty()) View.GONE else View.VISIBLE
        binding.contenedorRecientes.removeAllViews()

        recientes.forEach { consulta ->
            binding.contenedorRecientes.addView(chip(consulta) {
                binding.searchInput.setText(consulta)
                binding.searchInput.setSelection(consulta.length)
                esconderTeclado()
            })
        }
    }

    private fun pintarGeneros(generos: List<GeneroExplorable>) {
        binding.seccionGeneros.visibility = if (generos.isEmpty()) View.GONE else View.VISIBLE
        binding.contenedorGeneros.removeAllViews()

        generos.forEach { genero ->
            binding.contenedorGeneros.addView(chip(genero.nombre) {
                explorarViewModel.explorarGenero(genero)
                // El nombre va a la barra solo para que se vea que estas mirando, pero sin
                // volver a buscar: los resultados los trae explorarGenero, por genero de
                // verdad y no por lo que ponga en el titulo.
                textoPuestoPorCodigo = true
                binding.searchInput.setText(genero.nombre)
                esconderTeclado()
                decidirQueSeVe()
            })
        }
    }

    /** Un chip suelto, del mismo estilo que los de filtrar. */
    private fun chip(texto: String, alTocar: () -> Unit): TextView {
        val vista = layoutInflater.inflate(R.layout.item_chip, binding.contenedorRecientes, false)
        return (vista as TextView).apply {
            this.text = texto
            setOnClickListener { alTocar() }
        }
    }

    private fun sorprender() {
        val elegida = explorarViewModel.unaAlAzar()
        if (elegida == null) {
            Avisos.breve(binding.root, "Todavía no hay nada que proponerte")
            return
        }
        abrir(elegida)
    }

    // ---------------- Ir a la ficha ----------------

    private fun abrir(hallazgo: Hallazgo) {
        val destino = when (hallazgo.tipo) {
            TipoDeHallazgo.PELICULA ->
                Intent(context, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, hallazgo.id)
            TipoDeHallazgo.SERIE ->
                Intent(context, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, hallazgo.id)
            TipoDeHallazgo.PERSONA ->
                Intent(context, PersonaA::class.java).putExtra(PersonaA.EXTRA_PERSONA_ID, hallazgo.id)
        }
        startActivity(destino)
    }

    private fun esconderTeclado() {
        val teclado = requireContext().getSystemService(InputMethodManager::class.java)
        teclado?.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
