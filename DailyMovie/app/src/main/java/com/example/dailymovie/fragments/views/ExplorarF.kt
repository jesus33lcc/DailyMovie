package com.example.dailymovie.fragments.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.activities.views.MovieA
import com.example.dailymovie.activities.views.PersonaA
import com.example.dailymovie.activities.views.SagaA
import com.example.dailymovie.activities.views.SerieA
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.utils.abrirConCartel
import com.example.dailymovie.utils.siLaVistaSigueAhi
import com.example.dailymovie.databinding.FragmentExplorarBinding
import com.example.dailymovie.fragments.viewmodels.ExplorarViewModel
import com.example.dailymovie.fragments.viewmodels.OrdenDeBusqueda
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.FiltrosAvanzados
import com.example.dailymovie.models.GeneroExplorable
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.utils.BusquedasRecientes
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos
import com.example.dailymovie.utils.DialogoDailyMovie

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
        explorarViewModel.cargarPlataformas()
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
        resultadosAdapter = HallazgoAdapter { hallazgo, cartel -> abrir(hallazgo, cartel) }
        binding.rvListaBusqueda.layoutManager = GridLayoutManager(context, columnasQueCaben())
        binding.rvListaBusqueda.adapter = resultadosAdapter
        binding.rvListaBusqueda.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))

        // Cargar mas segun se baja. Se avisa con una fila de margen para que la siguiente
        // tanda este pedida antes de que el usuario llegue al final y vea el hueco.
        binding.rvListaBusqueda.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(lista: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val gestor = lista.layoutManager as? GridLayoutManager ?: return
                val ultimoVisible = gestor.findLastVisibleItemPosition()
                if (ultimoVisible >= gestor.itemCount - gestor.spanCount * 2) {
                    explorarViewModel.cargarMas()
                }
            }
        })
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
            // El de sagas se pintaba con su contador pero no tenia listener, asi que salia un
            // chip que no hacia nada al tocarlo.
            binding.chipSagas to TipoDeHallazgo.SAGA,
            binding.chipPersonas to TipoDeHallazgo.PERSONA
        ).forEach { (chip, tipo) ->
            chip.setOnClickListener {
                volverArriba = true
                explorarViewModel.cambiarFiltro(explorarViewModel.filtro.copy(tipo = tipo))
                pintarChips()
            }
        }

        binding.btnFiltros.setOnClickListener { mostrarFiltros() }

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

    /**
     * El dialogo de filtros finos: año, nota minima y plataforma.
     *
     * Se avisa dentro de que con filtros puestos manda el catalogo y no lo escrito arriba,
     * porque discover no sabe buscar por titulo y si no se dice parece que la busqueda se ha
     * roto.
     */
    private fun mostrarFiltros() {
        val contenido = layoutInflater.inflate(R.layout.dialogo_filtros, null)
        val campoAno = contenido.findViewById<EditText>(R.id.campoAno)
        val puestos = explorarViewModel.filtrosAvanzados

        campoAno.setText(puestos.ano?.toString().orEmpty())

        // Nota minima
        var notaElegida = puestos.notaMinima
        val chipsDeNota = listOf(
            contenido.findViewById<TextView>(R.id.chipNotaCualquiera) to (null as Double?),
            contenido.findViewById<TextView>(R.id.chipNota6) to 6.0,
            contenido.findViewById<TextView>(R.id.chipNota7) to 7.0,
            contenido.findViewById<TextView>(R.id.chipNota8) to 8.0
        )
        fun pintarNota() = chipsDeNota.forEach { (chip, nota) -> chip.isSelected = notaElegida == nota }
        chipsDeNota.forEach { (chip, nota) ->
            chip.setOnClickListener { notaElegida = nota; pintarNota() }
        }
        pintarNota()

        // Plataformas del pais
        var plataformaElegida = puestos.plataforma
        var nombrePlataforma = puestos.nombrePlataforma
        val contenedor = contenido.findViewById<LinearLayout>(R.id.contenedorPlataformas)
        val chipsDePlataforma = mutableMapOf<Int?, TextView>()

        fun pintarPlataformas() =
            chipsDePlataforma.forEach { (id, chip) -> chip.isSelected = plataformaElegida == id }

        fun anadirChipDePlataforma(id: Int?, nombre: String) {
            val chip = chip(nombre) {
                plataformaElegida = id
                nombrePlataforma = if (id == null) null else nombre
                pintarPlataformas()
            }
            chipsDePlataforma[id] = chip
            contenedor.addView(chip)
        }

        anadirChipDePlataforma(null, "Cualquiera")
        explorarViewModel.plataformas.value.orEmpty().take(12).forEach {
            anadirChipDePlataforma(it.id, it.nombre)
        }
        pintarPlataformas()
        contenido.findViewById<View>(R.id.tituloPlataformas).visibility =
            if (chipsDePlataforma.size > 1) View.VISIBLE else View.GONE

        DialogoDailyMovie.mostrar(
            context = requireContext(),
            titulo = "Filtrar",
            contenido = contenido,
            textoAceptar = "Aplicar",
            textoCancelar = "Quitar filtros",
            alCancelar = {
                explorarViewModel.cambiarFiltrosAvanzados(FiltrosAvanzados())
                pintarBotonDeFiltros()
            }
        ) { dialogo ->
            dialogo.dismiss()
            explorarViewModel.cambiarFiltrosAvanzados(
                FiltrosAvanzados(
                    ano = campoAno.text.toString().toIntOrNull(),
                    notaMinima = notaElegida,
                    plataforma = plataformaElegida,
                    nombrePlataforma = nombrePlataforma
                )
            )
            pintarBotonDeFiltros()
            esconderTeclado()
            decidirQueSeVe()
        }
    }

    /**
     * El boton de filtros se pinta en menta cuando hay alguno puesto.
     *
     * Sin esto no habia forma de saber que la busqueda venia recortada: se veian veinte
     * resultados y parecia que no habia mas, cuando en realidad estaba filtrando por nota.
     */
    private fun pintarBotonDeFiltros() {
        val hayFiltros = !explorarViewModel.filtrosAvanzados.estanVacios()
        binding.btnFiltros.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (hayFiltros) R.color.colorPrimary else R.color.white
            )
        )
    }

    private fun pintarChips() {
        val filtro = explorarViewModel.filtro.tipo
        listOf(
            Triple(binding.chipTodo, null as TipoDeHallazgo?, "Todo"),
            Triple(binding.chipPeliculas, TipoDeHallazgo.PELICULA, "Películas"),
            Triple(binding.chipSeries, TipoDeHallazgo.SERIE, "Series"),
            Triple(binding.chipSagas, TipoDeHallazgo.SAGA, "Sagas"),
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
                // El diff corre en otro hilo y este callback llega despues: para entonces la
                // vista puede haber desaparecido.
                if (!volverArriba) return@submitList
                volverArriba = false
                _binding.siLaVistaSigueAhi { vista -> vista.rvListaBusqueda.scrollToPosition(0) }
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
            val adaptador = HallazgoAdapter { hallazgo, cartel -> abrir(hallazgo, cartel) }
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
            val adaptadorHistorial = HallazgoAdapter { hallazgo, cartel -> abrir(hallazgo, cartel) }
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
        val buscando = explorarViewModel.hayBusquedaOFiltros()
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

    private fun abrir(hallazgo: Hallazgo, cartel: View? = null) {
        // Abrir un resultado tambien cuenta como buscar a proposito: hasta ahora solo se
        // guardaba al pulsar la lupa del teclado, y casi nadie la pulsa. Se escribe, se mira
        // lo que sale y se toca, asi que "lo ultimo que buscaste" salia casi siempre vacio.
        val consulta = binding.searchInput.text.toString().trim()
        if (consulta.length >= MINIMO_PARA_GUARDAR) {
            BusquedasRecientes.guardar(requireContext(), consulta)
        }

        val destino = when (hallazgo.tipo) {
            TipoDeHallazgo.PELICULA ->
                Intent(context, MovieA::class.java).putExtra(MovieA.EXTRA_MOVIE_ID, hallazgo.id)
            TipoDeHallazgo.SERIE ->
                Intent(context, SerieA::class.java).putExtra(SerieA.EXTRA_SERIE_ID, hallazgo.id)
            TipoDeHallazgo.PERSONA ->
                Intent(context, PersonaA::class.java).putExtra(PersonaA.EXTRA_PERSONA_ID, hallazgo.id)
            TipoDeHallazgo.SAGA ->
                Intent(context, SagaA::class.java)
                    .putExtra(SagaA.EXTRA_SAGA_ID, hallazgo.id)
                    .putExtra(SagaA.EXTRA_NOMBRE, hallazgo.titulo)
        }
        if (cartel == null) {
            startActivity(destino)
        } else {
            requireActivity()
                .abrirConCartel(destino, cartel, HallazgoAdapter.nombreDeTransicion(hallazgo))
        }
    }

    private companion object {
        /** Con menos de tres letras la busqueda no se lanza, asi que tampoco se guarda. */
        const val MINIMO_PARA_GUARDAR = 3
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
