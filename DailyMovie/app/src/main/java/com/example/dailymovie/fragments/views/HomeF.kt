package com.example.dailymovie.fragments.views

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.activities.views.SerieA
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotogramaDeUrl
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.utils.abrirFicha
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.databinding.FragmentHomeBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.models.MovieOfTheDay
import com.example.dailymovie.utils.Trailers
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.fragments.viewmodels.HomeViewModel
import com.example.dailymovie.activities.views.MovieA
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.dailymovie.utils.Avisos

class HomeF : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /**
     * Si todavia no ha llegado nada.
     *
     * Marca la diferencia entre la primera carga, donde se enseñan los huecos con brillo, y
     * un refresco posterior, donde ya hay carteles y lo que toca es la rueda de arriba.
     */
    private var primeraCarga = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        binding.recyclerViewNowPlaying.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewNowPlaying.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
        binding.recyclerViewPopular.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewPopular.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
        binding.recyclerViewTopRated.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewTopRated.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
        binding.recyclerViewUpcoming.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewUpcoming.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))

        // Cada fila apaga su propio hueco cuando le llegan sus peliculas, en vez de esperar
        // a que terminen las cuatro: si una tarda mas, las demas ya se ven.
        homeViewModel.nowPlayingMovies.observe(viewLifecycleOwner, Observer { movies ->
            pintarTira(binding.recyclerViewNowPlaying, movies.map { Hallazgo.de(it) })
            apagarFantasma(binding.fantasmaNowPlaying.root)
        })
        homeViewModel.popularMovies.observe(viewLifecycleOwner, Observer { movies ->
            pintarTira(binding.recyclerViewPopular, movies.map { Hallazgo.de(it) })
            apagarFantasma(binding.fantasmaPopular.root)
        })
        homeViewModel.topRatedMovies.observe(viewLifecycleOwner, Observer { movies ->
            pintarTira(binding.recyclerViewTopRated, movies.map { Hallazgo.de(it) })
            apagarFantasma(binding.fantasmaTopRated.root)
        })
        homeViewModel.upcomingMovies.observe(viewLifecycleOwner, Observer { movies ->
            pintarTira(binding.recyclerViewUpcoming, movies.map { Hallazgo.de(it) })
            apagarFantasma(binding.fantasmaUpcoming.root)
        })

        homeViewModel.movieOfTheDay.observe(viewLifecycleOwner, Observer { movie ->
            movie?.let {
                Log.d("HomeF", "La pelicula del dia es: ${it.title}")
                displayMovieOfTheDay(it)
            } ?: Log.d("HomeF", "No hay pelicula del dia")
        })

        homeViewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Avisos.breve(binding.root, getString(it.mensaje()))
                homeViewModel.errorMostrado()
            }
        })

        homeViewModel.seriesEmpezadas.observe(viewLifecycleOwner) { empezadas ->
            binding.seccionSigueViendo.visibility =
                if (empezadas.isEmpty()) View.GONE else View.VISIBLE
            // En esta fila el año de estreno no dice nada: lo que interesa es por donde vas.
            pintarTira(
                binding.recyclerSigueViendo,
                empezadas.map { it.serie.copy(subtituloLibre = it.comoVas()) }
            )
        }

        homeViewModel.cargando.observe(viewLifecycleOwner, Observer { cargando ->
            // La rueda de "tirar para refrescar" solo cuando ya hay algo en pantalla: la
            // primera vez el que avisa de que se esta cargando es el hueco con brillo.
            binding.swipeRefreshLayout.isRefreshing = cargando && !primeraCarga
        })

        homeViewModel.cargarPortada()



        setupSwipeRefreshLayout()
    }


    /**
     * Pinta una tira con la tarjeta comun.
     *
     * Es la misma que usa el buscador: cartel, titulo, año y nota, con la etiqueta de si es
     * pelicula o serie y el aviso de "Proximamente" en lo que aun no ha salido. Antes cada
     * pantalla tenia su propio adaptador haciendo lo mismo con otro diseño.
     */
    private fun pintarTira(lista: RecyclerView, hallazgos: List<Hallazgo>) {
        if (lista.itemDecorationCount == 0) {
            lista.addItemDecoration(SpacingItemDecoration.deLista(requireContext()))
        }
        // Las cuatro filas fijas lo tienen puesto arriba; "Sigue viendo" aparece y desaparece,
        // asi que se le pone aqui la primera vez que le toca pintar algo.
        if (lista.layoutManager == null) {
            lista.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        val adaptador = lista.adapter as? HallazgoAdapter
            ?: HallazgoAdapter { hallazgo, cartel -> requireActivity().abrirFicha(hallazgo, cartel) }.also { lista.adapter = it }
        adaptador.submitList(hallazgos)
    }


    /**
     * Quita el hueco con brillo de una fila, ya con sus peliculas dentro.
     *
     * El brillo se para ademas de esconderse: un ShimmerFrameLayout invisible sigue animando
     * y gastando bateria si solo se le pone GONE.
     */
    private fun apagarFantasma(fantasma: View) {
        if (fantasma.visibility == View.GONE) return
        (fantasma as? ShimmerFrameLayout)?.stopShimmer()
        fantasma.visibility = View.GONE
        primeraCarga = false
    }

    private fun setupSwipeRefreshLayout() {
        // El circulo se para solo cuando el ViewModel avisa de que ha contestado todo.
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.cargarPortada()
        }
    }

    private fun displayMovieOfTheDay(movie: MovieOfTheDay) {
        binding.movieTitle.text = movie.title
        binding.movieReview.text = movie.review

        // La fecha es la del dia que se curo; en una recomendada no significa nada.
        val esCurada = !movie.author.isNullOrBlank()
        binding.lblFecha.visibility = if (esCurada) View.VISIBLE else View.GONE
        binding.movieDate.visibility = if (esCurada) View.VISIBLE else View.GONE
        binding.movieDate.text =
            if (esCurada) SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) else ""

        // La curada a mano la firma alguien; la recomendada no tiene autor, tiene motivo.
        // Solo se enseña lo que haya, y nunca los dos a la vez.
        val hayAutor = !movie.author.isNullOrBlank()
        binding.lblAutor.visibility = if (hayAutor) View.VISIBLE else View.GONE
        binding.movieAuthor.visibility = if (hayAutor) View.VISIBLE else View.GONE
        binding.movieAuthor.text = movie.author.orEmpty()

        val hayMotivo = !movie.motivo.isNullOrBlank()
        binding.txtMotivo.visibility = if (hayMotivo) View.VISIBLE else View.GONE
        binding.txtMotivo.text = movie.motivo.orEmpty()

        mostrarTrailer(movie.videoId)

        binding.btnViewFullDetails.setOnClickListener {
            val intent = Intent(context, MovieA::class.java)
            intent.putExtra(MovieA.EXTRA_MOVIE_ID, movie.id)
            startActivity(intent)
        }
    }

    /**
     * Enseña la miniatura del trailer, o quita el hueco si esa pelicula no tiene ninguno.
     *
     * YouTube ya no deja reproducir dentro de un WebView de Android, asi que al tocar la
     * miniatura se abre YouTube. Ver utils/Trailers.
     */
    private fun mostrarTrailer(videoId: String) {
        if (videoId.isBlank()) {
            binding.trailerContainer.visibility = View.GONE
            return
        }

        binding.trailerContainer.visibility = View.VISIBLE
        binding.trailerThumbnail.cargarFotogramaDeUrl(Trailers.miniatura(videoId))

        binding.trailerContainer.setOnClickListener {
            Trailers.abrir(binding.root, videoId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
