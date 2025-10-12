package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.adapters.*
import com.example.dailymovie.client.response.*
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.MovieDetailsModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.activities.viewmodels.MovieViewModel
import com.example.dailymovie.databinding.ActivityMovieBinding
import com.example.dailymovie.adapters.ImagenAdapter
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.utils.LocaleUtil
import com.example.dailymovie.utils.mensaje

class MovieA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityMovieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieId = intent.getIntExtra("MOVIE_ID", -1)

        if (movieId != -1) {
            movieViewModel.cargarPelicula(movieId)

            movieViewModel.movieDetails.observe(this, Observer { movieDetailsResponse ->
                movieDetailsResponse?.let {
                    val movieDetails = convertToMovieDetailsModel(it)
                    displayMovieDetails(movieDetails)
                    setupButtons(movieDetails)
                    mostrarExtras(it)
                }
            })

            movieViewModel.movieProviders.observe(this, Observer { providerResponse ->
                providerResponse?.let {
                    displayProviders(it)
                }
            })

            movieViewModel.movieCredits.observe(this, Observer { creditResponse ->
                creditResponse?.let {
                    displayCredits(it)
                    mostrarDirector(it)
                }
            })

            movieViewModel.movieVideos.observe(this, Observer { videoList ->
                videoList?.let {
                    displayVideos(it)
                }
            })

            movieViewModel.similarMovies.observe(this, Observer { similarMovies ->
                similarMovies?.let {
                    displaySimilarMovies(it)
                }
            })

            movieViewModel.recommendedMovies.observe(this, Observer { recommendedMovies ->
                recommendedMovies?.let {
                    displayRecommendedMovies(it)
                }
            })

            movieViewModel.error.observe(this, Observer { error ->
                error?.let {
                    showToast(getString(it.mensaje()))
                    movieViewModel.errorMostrado()
                }
            })
        } else {
            showToast("Error: El Id de la pelicula no es valido.")
        }
    }

    private fun setupButtons(movie: MovieDetailsModel) {
        val movieModel = MovieModel(
            id = movie.id,
            title = movie.title,
            releaseDate = movie.releaseDate,
            voteAverage = movie.voteAverage,
            posterPath = movie.posterPath
        )

        val btnFavorite: ImageButton = findViewById(R.id.btnFavorite)
        val btnWatched: ImageButton = findViewById(R.id.btnWatched)
        val btnAddList: ImageButton = findViewById(R.id.btnAddList)
        val btnShare: ImageButton = findViewById(R.id.btnShare)

        updateFavoriteButtonIcon(movie.id, btnFavorite)
        updateWatchedButtonIcon(movie.id, btnWatched)

        btnFavorite.setOnClickListener {
            movieViewModel.toggleFavorite(movieModel) { isFavorite ->
                updateFavoriteButtonIcon(movie.id, btnFavorite)
            }
        }

        btnWatched.setOnClickListener {
            movieViewModel.toggleWatched(movieModel) { isWatched ->
                updateWatchedButtonIcon(movie.id, btnWatched)
            }
        }
        btnAddList.setOnClickListener {
            showListSelectionDialog(movieModel)
        }
        btnShare.setOnClickListener {
            shareMovie(movieModel)
        }
    }

    /** Abre otra pelicula desde las tiras de similares y recomendadas. */
    private fun abrirOtra(hallazgo: Hallazgo) {
        startActivity(Intent(this, MovieA::class.java).putExtra("MOVIE_ID", hallazgo.id))
    }

    private fun shareMovie(movie: MovieModel) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Te recomiendo esta pelicula: ${movie.title}\n${Constantes.BASE_MOVIE_URL}${movie.id}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir pelicula via"))
    }

    private fun updateFavoriteButtonIcon(movieId: Int, button: ImageButton) {
        movieViewModel.esFavorita(movieId) { isFavorite ->
            runOnUiThread {
                val icon = if (isFavorite) R.drawable.ic_baseline_favorite_24 else R.drawable.ic_baseline_favorite_border_24
                button.setImageResource(icon)
            }
        }
    }

    private fun updateWatchedButtonIcon(movieId: Int, button: ImageButton) {
        movieViewModel.estaVista(movieId) { isWatched ->
            runOnUiThread {
                val icon = if (isWatched) R.drawable.ic_baseline_visibility_24 else R.drawable.ic_baseline_visibility_off_24
                button.setImageResource(icon)
            }
        }
    }

    /**
     * Guardar la pelicula en las listas del usuario.
     *
     * Antes era un menu de una sola eleccion: tocabas una lista, se cerraba, y para meterla
     * en dos sitios habia que abrirlo otra vez. Tampoco se veia donde la tenias ya guardada
     * ni se podia sacar desde aqui.
     *
     * Ahora salen todas con su casilla, las que ya la tienen vienen marcadas, y al guardar
     * se aplica de una vez lo que hayas marcado y lo que hayas quitado.
     */
    private fun showListSelectionDialog(movie: MovieModel) {
        movieViewModel.getCustomLists { nombres ->
            if (nombres.isEmpty()) {
                DialogoDailyMovie.mostrar(
                    context = this,
                    titulo = "Guardar en una lista",
                    mensaje = "Todavía no tienes ninguna lista tuya. Créala en la pestaña " +
                        "Listas y aquí podrás guardar lo que quieras.",
                    textoAceptar = "Entendido",
                    textoCancelar = null
                ) { it.dismiss() }
                return@getCustomLists
            }

            movieViewModel.listasConLaPelicula(movie.id) { yaEstaba ->
                val marcadas = yaEstaba.toMutableSet()
                val contenido = layoutInflater.inflate(R.layout.dialogo_listas, null)
                val lista = contenido.findViewById<RecyclerView>(R.id.dialogoListas)
                lista.layoutManager = LinearLayoutManager(this)
                lista.adapter = ListaCasillaAdapter(nombres, marcadas)

                DialogoDailyMovie.mostrar(
                    context = this,
                    titulo = "Guardar en una lista",
                    mensaje = movie.title,
                    contenido = contenido,
                    textoAceptar = "Guardar"
                ) { dialogo ->
                    dialogo.dismiss()
                    aplicarCambiosDeListas(movie, yaEstaba, marcadas)
                }
            }
        }
    }

    /** Solo se toca lo que ha cambiado: lo que se ha marcado y lo que se ha desmarcado. */
    private fun aplicarCambiosDeListas(
        movie: MovieModel,
        antes: Set<String>,
        ahora: Set<String>
    ) {
        val meter = ahora - antes
        val sacar = antes - ahora
        if (meter.isEmpty() && sacar.isEmpty()) return

        meter.forEach { movieViewModel.addMovieToList(it, movie) { } }
        sacar.forEach { movieViewModel.removeMovieFromList(it, movie) { } }

        val aviso = when {
            meter.isNotEmpty() && sacar.isEmpty() -> "Guardada en ${meter.joinToString(", ")}"
            meter.isEmpty() && sacar.isNotEmpty() -> "Quitada de ${sacar.joinToString(", ")}"
            else -> "Listas actualizadas"
        }
        showToast(aviso)
    }

    private fun convertToMovieDetailsModel(response: MovieDetailsResponse): MovieDetailsModel {
        return MovieDetailsModel(
            adult = response.adult,
            backdropPath = response.backdropPath,
            belongsToCollection = response.belongsToCollection,
            budget = response.budget,
            genres = response.genres,
            homepage = response.homepage,
            id = response.id,
            imdbId = response.imdbId,
            originCountry = response.originCountry,
            originalLanguage = response.originalLanguage,
            originalTitle = response.originalTitle,
            overview = response.overview,
            popularity = response.popularity,
            posterPath = response.posterPath,
            productionCompanies = response.productionCompanies,
            releaseDate = response.releaseDate,
            revenue = response.revenue,
            runtime = response.runtime,
            status = response.status,
            tagline = response.tagline,
            title = response.title,
            video = response.video,
            voteAverage = response.voteAverage,
            voteCount = response.voteCount
        )
    }

    private fun displayMovieDetails(movie: MovieDetailsModel) {
        binding.txtTituloMovie.text = movie.title
        binding.txtTaglineMovie.text = movie.tagline
        binding.txtTaglineMovie.visibility = if (movie.tagline.isNullOrEmpty()) View.GONE else View.VISIBLE

        movieViewModel.movieCredits.value?.let { mostrarDirector(it) }

        // Si aun no ha salido se dice, y se enseña la fecha entera en vez del año suelto:
        // "2026" no aclara si ya se puede ver o si queda medio año.
        val porVenir = Fechas.estaPorVenir(movie.releaseDate)
        val releaseYear = movie.releaseDate.take(4)
        binding.txtAnioMovie.text = if (porVenir) {
            getString(R.string.proximamente, Fechas.enLargo(movie.releaseDate))
        } else {
            releaseYear
        }
        // La etiqueta "Año:" sobra cuando el texto ya empieza por "Próximamente".
        binding.txtAnioLabel.visibility =
            if (releaseYear.isEmpty() || porVenir) View.GONE else View.VISIBLE
        binding.txtAnioMovie.visibility = if (releaseYear.isEmpty()) View.GONE else View.VISIBLE

        binding.txtValoracionLabel.visibility = if (movie.voteAverage == 0.0) View.GONE else View.VISIBLE
        binding.txtValoracionMovie.text = movie.voteAverage.toString()
        binding.txtValoracionMovie.visibility = if (movie.voteAverage == 0.0) View.GONE else View.VISIBLE

        binding.txtOverviewLabel.visibility = if (movie.overview.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.txtOverviewMovie.text = movie.overview
        binding.txtOverviewMovie.visibility = if (movie.overview.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.txtGenresLabel.visibility = if (movie.genres.isEmpty()) View.GONE else View.VISIBLE
        binding.txtGenresMovie.text = movie.genres.joinToString { it.name }
        binding.txtGenresMovie.visibility = if (movie.genres.isEmpty()) View.GONE else View.VISIBLE

        binding.txtRuntimeLabel.visibility = if (movie.runtime == 0) View.GONE else View.VISIBLE
        binding.txtRuntimeMovie.text = "${movie.runtime} min"
        binding.txtRuntimeMovie.visibility = if (movie.runtime == 0) View.GONE else View.VISIBLE

        binding.txtBudgetLabel.visibility = if (movie.budget == 0) View.GONE else View.VISIBLE
        binding.txtBudgetMovie.text = "${movie.budget} USD"
        binding.txtBudgetMovie.visibility = if (movie.budget == 0) View.GONE else View.VISIBLE

        binding.txtRevenueLabel.visibility = if (movie.revenue == 0L) View.GONE else View.VISIBLE
        binding.txtRevenueMovie.text = "${movie.revenue} USD"
        binding.txtRevenueMovie.visibility = if (movie.revenue == 0L) View.GONE else View.VISIBLE

        binding.imgPosterMovie.cargarCartel(movie.posterPath)

        val hasContent = binding.txtDirectorMovie.text.isNotEmpty() ||
                binding.txtRuntimeMovie.text.isNotEmpty() ||
                binding.txtAnioMovie.text.isNotEmpty() ||
                binding.txtValoracionMovie.text.isNotEmpty() ||
                binding.txtOverviewMovie.text.isNotEmpty() ||
                binding.txtGenresMovie.text.isNotEmpty() ||
                binding.txtBudgetMovie.text.isNotEmpty() ||
                binding.txtRevenueMovie.text.isNotEmpty()

        binding.sectionDetails.visibility = if (hasContent) View.VISIBLE else View.GONE
    }

    /**
     * Enseña al director y deja tocarlo para ir a su ficha.
     *
     * Antes solo se escribia el nombre, y era raro que el reparto se pudiera abrir y el
     * director no, siendo normalmente el nombre por el que buscas una pelicula.
     */
    private fun mostrarDirector(creditos: CreditResponse) {
        val director = creditos.crew.firstOrNull { it.job == "Director" }
        binding.txtDirectorMovie.text = director?.name ?: getDirectorName(creditos)

        if (director != null) {
            binding.txtDirectorMovie.setOnClickListener {
                startActivity(
                    Intent(this, PersonaA::class.java)
                        .putExtra(PersonaA.EXTRA_PERSONA_ID, director.id)
                )
            }
        }
    }

    private fun getDirectorName(credits: CreditResponse?): String {
        credits?.let {
            it.crew.forEach { crewMember ->
                if (crewMember.job == "Director") {
                    return crewMember.name
                }
            }
            it.cast.forEach { castMember ->
                if (castMember.character == "Director") {
                    return castMember.name
                }
            }
        }
        return ""
    }

    private fun displayProviders(providerResponse: ProviderResponse) {
        val providers = providerResponse.results[LocaleUtil.getDeviceCountry()]?.flatrate ?: emptyList()
        if (providers.isNotEmpty()) {
            val adapter = ProviderAdapter(providers)
            binding.recyclerViewProviders.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewProviders.adapter = adapter
            binding.sectionProviders.visibility = View.VISIBLE
            binding.recyclerViewProviders.addItemDecoration(SpacingItemDecoration.deLista(this))
        } else {
            binding.sectionProviders.visibility = View.GONE
        }
    }

    private fun displayCredits(creditResponse: CreditResponse) {
        val cast = creditResponse.cast
        if (cast.isNotEmpty()) {
            val adapter = CreditAdapter(cast)
            binding.recyclerViewCredits.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewCredits.adapter = adapter
            binding.sectionCredits.visibility = View.VISIBLE
            binding.recyclerViewCredits.addItemDecoration(SpacingItemDecoration.deLista(this))
        } else {
            binding.sectionCredits.visibility = View.GONE
        }
    }

    private fun displayVideos(videoList: List<VideoModel>) {
        if (videoList.isNotEmpty()) {
            // Los trailers van en tira horizontal, como las imagenes. Antes era un ViewPager
            // a pantalla completa: se veia un video a la vez y estirado.
            binding.recyclerVideos.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerVideos.adapter = VideoAdapter(videoList)
            if (binding.recyclerVideos.itemDecorationCount == 0) {
                binding.recyclerVideos.addItemDecoration(SpacingItemDecoration.deLista(this))
            }
            binding.sectionVideos.visibility = View.VISIBLE
        } else {
            binding.sectionVideos.visibility = View.GONE
        }
    }

    private fun displaySimilarMovies(similarMovies: List<MovieModel>) {
        if (similarMovies.isNotEmpty()) {
            val adapter = HallazgoAdapter { abrirOtra(it) }.also { it.submitList(similarMovies.map { p -> Hallazgo.de(p) }) }
            binding.recyclerViewSimilarMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewSimilarMovies.adapter = adapter
            binding.sectionSimilarMovies.visibility = View.VISIBLE
            binding.recyclerViewSimilarMovies.addItemDecoration(SpacingItemDecoration.deLista(this))
        } else {
            binding.sectionSimilarMovies.visibility = View.GONE
        }
    }

    private fun displayRecommendedMovies(recommendedMovies: List<MovieModel>) {
        if (recommendedMovies.isNotEmpty()) {
            val adapter = HallazgoAdapter { abrirOtra(it) }.also { it.submitList(recommendedMovies.map { p -> Hallazgo.de(p) }) }
            binding.recyclerViewRecommendedMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recyclerViewRecommendedMovies.adapter = adapter
            binding.sectionRecommendedMovies.visibility = View.VISIBLE
            binding.recyclerViewRecommendedMovies.addItemDecoration(SpacingItemDecoration.deLista(this))
        } else {
            binding.sectionRecommendedMovies.visibility = View.GONE
        }
    }

    /**
     * Lo que TMDB da y antes no se usaba: imagenes, clasificacion por edad y ficha en IMDb.
     *
     * Llega en la misma peticion que los detalles gracias a append_to_response, asi que no
     * cuesta ni un viaje mas a la API.
     */
    private fun mostrarExtras(detalles: MovieDetailsResponse) {
        mostrarImagenes(detalles)
        mostrarClasificacionPorEdad(detalles)
    }

    private fun mostrarImagenes(detalles: MovieDetailsResponse) {
        // Se cogen los fondos y no los carteles: son fotogramas de la pelicula, que es lo
        // que apetece mirar, y ademas son apaisados y cuadran con la tira horizontal.
        val fondos = detalles.imagenes?.fondos.orEmpty().map { it.ruta }
        if (fondos.isEmpty()) {
            binding.sectionImagenes.visibility = View.GONE
            return
        }
        binding.sectionImagenes.visibility = View.VISIBLE
        binding.recyclerImagenes.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerImagenes.addItemDecoration(SpacingItemDecoration.deLista(this))
        binding.recyclerImagenes.adapter = ImagenAdapter(fondos)
    }

    /**
     * La clasificacion cambia en cada pais, asi que se busca la del pais del dispositivo.
     * Si esa pelicula no esta clasificada aqui, no se enseña nada en vez de poner la de
     * otro pais, que solo confundiria.
     */
    private fun mostrarClasificacionPorEdad(detalles: MovieDetailsResponse) {
        val delPais = detalles.fechasDeEstreno?.porPais
            ?.firstOrNull { it.pais == LocaleUtil.getDeviceCountry() }
        val clasificacion = delPais?.estrenos
            ?.firstOrNull { !it.clasificacion.isNullOrBlank() }
            ?.clasificacion

        if (clasificacion.isNullOrBlank()) {
            binding.txtClasificacionEdad.visibility = View.GONE
        } else {
            binding.txtClasificacionEdad.visibility = View.VISIBLE
            binding.txtClasificacionEdad.text = clasificacion
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
