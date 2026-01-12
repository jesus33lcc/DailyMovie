package com.example.dailymovie.activities.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.adapters.*
import com.example.dailymovie.client.response.*
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.models.MovieDetailsModel
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.models.VideoModel
import com.example.dailymovie.utils.Cifras
import android.net.Uri
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.rebotar
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.activities.viewmodels.MovieViewModel
import com.example.dailymovie.adapters.ResenaAdapter
import com.example.dailymovie.utils.avisoDeListas
import com.example.dailymovie.utils.elegirListas
import com.example.dailymovie.utils.compartirRecomendacion
import com.example.dailymovie.utils.menuDeEnlaces
import com.example.dailymovie.utils.abrirFicha
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.databinding.ActivityMovieBinding
import com.example.dailymovie.adapters.ImagenAdapter
import com.example.dailymovie.graphics.mostrarSi
import com.example.dailymovie.graphics.ponerOEsconder
import com.example.dailymovie.graphics.SpacingItemDecoration
import com.example.dailymovie.utils.DialogoDailyMovie
import com.example.dailymovie.utils.LocaleUtil
import com.example.dailymovie.utils.mensaje
import com.example.dailymovie.utils.Avisos

class MovieA : AppCompatActivity() {

    private val movieViewModel: MovieViewModel by viewModels()
    private lateinit var binding: ActivityMovieBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieId = intent.getIntExtra(EXTRA_MOVIE_ID, -1)
        // Sin un id no hay nada que enseñar. Antes se avisaba y se dejaba una pantalla vacia
        // y muerta; las fichas de serie y de persona ya cerraban en este caso.
        if (movieId == -1) {
            showToast("No hemos podido abrir esa película")
            finish()
            return
        }

        // El mismo nombre que lleva el cartel de la tarjeta desde la que se ha llegado: es
        // lo que hace que la imagen crezca hasta aqui en vez de aparecer de golpe.
        binding.imgPosterMovie.transitionName = "cartel_${TipoDeHallazgo.PELICULA}_$movieId"

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

        movieViewModel.resenas.observe(this) { mostrarResenas(it) }

        movieViewModel.recommendedMovies.observe(this, Observer { recommendedMovies ->
            recommendedMovies?.let {
                displayRecommendedMovies(it)
            }
        })

        // Los dos botones de guardar se pintan solos cada vez que cambia su estado, que
        // ahora vive en el ViewModel y no en Firestore.
        var primeraPintadaFavorita = true
        movieViewModel.favorita.puesto.observe(this) { puesto ->
            pintarBotonDeGuardado(
                binding.btnFavorite, puesto,
                R.drawable.ic_baseline_favorite_24,
                R.drawable.ic_baseline_favorite_border_24,
                primeraPintadaFavorita
            )
            primeraPintadaFavorita = false
        }

        var primeraPintadaVista = true
        movieViewModel.vista.puesto.observe(this) { puesto ->
            pintarBotonDeGuardado(
                binding.btnWatched, puesto,
                R.drawable.ic_baseline_visibility_24,
                R.drawable.ic_baseline_visibility_off_24,
                primeraPintadaVista
            )
            primeraPintadaVista = false
        }

        movieViewModel.error.observe(this, Observer { error ->
            error?.let {
                showToast(getString(it.mensaje()))
                movieViewModel.errorMostrado()
            }
        })
    }

    /**
     * Los cuatro botones de debajo del cartel.
     *
     * El corazon y el ojo ya no preguntan nada al pulsarse: el ViewModel lleva el estado en
     * memoria y esta pantalla solo pinta lo que le dice. Ver [com.example.dailymovie.activities.viewmodels.Marcado].
     */
    private fun setupButtons(movie: MovieDetailsModel) {
        val movieModel = MovieModel(
            id = movie.id,
            title = movie.title,
            releaseDate = movie.releaseDate,
            voteAverage = movie.voteAverage,
            posterPath = movie.posterPath
        )

        movieViewModel.prepararBotonesDeGuardado(movieModel)

        binding.btnFavorite.setOnClickListener { movieViewModel.cambiarFavorita() }
        binding.btnWatched.setOnClickListener { movieViewModel.cambiarVista() }
        binding.btnAddList.setOnClickListener { showListSelectionDialog(movieModel) }
        binding.btnShare.setOnClickListener { shareMovie(movieModel) }
    }

    /** Abre otra pelicula desde las tiras de similares y recomendadas. */
    private fun abrirOtra(hallazgo: Hallazgo, cartel: View) = abrirFicha(hallazgo, cartel)

    /**
     * El boton que lleva a la pelicula fuera de la app.
     *
     * TMDB devuelve el id de IMDb y la web oficial en la misma peticion de los detalles, asi
     * que no cuesta nada ofrecerlos: es donde la gente va a mirar la ficha "de verdad" o a
     * comprar la entrada. Si no viene ninguno de los dos, el boton ni aparece.
     */
    /** El boton que lleva la pelicula fuera de la app. */
    private fun prepararEnlaces(movie: MovieDetailsModel) {
        binding.btnEnlaces.visibility = View.VISIBLE
        binding.btnEnlaces.setOnClickListener { boton ->
            menuDeEnlaces(
                boton,
                imdbId = movie.imdbId,
                webOficial = movie.homepage,
                enTmdb = "${Constantes.BASE_MOVIE_URL}${movie.id}"
            )
        }
    }

    private fun shareMovie(movie: MovieModel) =
        compartirRecomendacion(movie.title, "${Constantes.BASE_MOVIE_URL}${movie.id}")

    /**
     * Pinta un boton de guardar y le da el saltito cuando cambia.
     *
     * El rebote se salta la primera vez: al abrir la ficha tambien se pinta, y ahi un boton
     * dando saltos solo distrae. De la segunda en adelante siempre es porque el usuario ha
     * pulsado.
     *
     * @param boton el corazon o el ojo.
     * @param puesto si toca pintarlo marcado.
     * @param marcado el dibujo de marcado.
     * @param sinMarcar el dibujo de sin marcar.
     * @param esLaPrimera si es la pintada de al abrir la ficha.
     */
    private fun pintarBotonDeGuardado(
        boton: ImageButton,
        puesto: Boolean,
        @DrawableRes marcado: Int,
        @DrawableRes sinMarcar: Int,
        esLaPrimera: Boolean
    ) {
        boton.setImageResource(if (puesto) marcado else sinMarcar)
        if (!esLaPrimera) boton.rebotar()
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
            movieViewModel.listasConLaPelicula(movie.id) { yaEstaba ->
                elegirListas(movie.title, nombres, yaEstaba) { meter, sacar ->
                    meter.forEach { movieViewModel.addMovieToList(it, movie) { } }
                    sacar.forEach { movieViewModel.removeMovieFromList(it, movie) { } }
                    showToast(avisoDeListas(meter, sacar))
                }
            }
        }
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

    /**
     * La saga a la que pertenece la pelicula, si pertenece a alguna.
     *
     * TMDB lo da en belongs_to_collection sin coste extra, asi que no hace falta pedir nada
     * mas: solo se enseña la fila y al tocarla se abre la saga entera.
     */
    private fun mostrarSaga(saga: com.example.dailymovie.models.CollectionModel?) {
        if (saga == null) {
            binding.sectionSaga.visibility = View.GONE
            return
        }
        binding.sectionSaga.visibility = View.VISIBLE
        binding.txtSaga.text = saga.name
        binding.imgSaga.cargarCartel(saga.posterPath)
        binding.sectionSaga.setOnClickListener {
            startActivity(
                Intent(this, SagaA::class.java)
                    .putExtra(SagaA.EXTRA_SAGA_ID, saga.id)
                    .putExtra(SagaA.EXTRA_NOMBRE, saga.name)
            )
        }
    }

    /**
     * Las reseñas que ha escrito la gente en TMDB.
     *
     * Se enseñan como mucho cinco: son textos largos y con mas de eso la ficha se convierte
     * en un foro. Quien quiera leerlas todas las tiene en la web de TMDB.
     */
    private fun mostrarResenas(resenas: List<ResenaResponse>) {
        if (resenas.isEmpty()) {
            binding.sectionResenas.visibility = View.GONE
            return
        }
        binding.sectionResenas.visibility = View.VISIBLE
        binding.recyclerViewResenas.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewResenas.adapter = ResenaAdapter(resenas.take(5))
    }

    private fun displayMovieDetails(movie: MovieDetailsModel) {
        binding.txtTituloMovie.text = movie.title
        binding.txtTaglineMovie.ponerOEsconder(movie.tagline)

        movieViewModel.movieCredits.value?.let { mostrarDirector(it) }

        // Si aun no ha salido se dice, y se enseña la fecha entera en vez del año suelto:
        // "2026" no aclara si ya se puede ver o si queda medio año.
        val porVenir = Fechas.estaPorVenir(movie.releaseDate)
        val ano = movie.releaseDate.take(4)
        binding.txtAnioMovie.ponerOEsconder(
            if (porVenir) getString(R.string.proximamente, Fechas.enLargo(movie.releaseDate)) else ano
        )
        // La etiqueta "Año:" sobra cuando el texto ya empieza por "Próximamente".
        binding.txtAnioLabel.mostrarSi(ano.isNotEmpty() && !porVenir)

        binding.txtValoracionMovie.ponerOEsconder(
            getString(R.string.nota_estrella, movie.voteAverage).takeIf { movie.voteAverage > 0 },
            binding.txtValoracionLabel
        )
        binding.txtOverviewMovie.ponerOEsconder(movie.overview, binding.txtOverviewLabel)
        binding.txtGenresMovie.ponerOEsconder(
            movie.genres.joinToString { it.name }.takeIf { movie.genres.isNotEmpty() },
            binding.txtGenresLabel
        )
        binding.txtRuntimeMovie.ponerOEsconder(
            Cifras.duracion(movie.runtime), binding.txtRuntimeLabel
        )
        binding.txtBudgetMovie.ponerOEsconder(
            Cifras.dinero(movie.budget.toLong()), binding.txtBudgetLabel
        )
        binding.txtRevenueMovie.ponerOEsconder(
            Cifras.dinero(movie.revenue), binding.txtRevenueLabel
        )

        binding.imgPosterMovie.cargarCartel(movie.posterPath)
        mostrarSaga(movie.belongsToCollection)
        prepararEnlaces(movie)

        val hasContent = binding.txtDirectorMovie.text.isNotEmpty() ||
                binding.txtRuntimeMovie.text.isNotEmpty() ||
                binding.txtAnioMovie.text.isNotEmpty() ||
                binding.txtValoracionMovie.text.isNotEmpty() ||
                binding.txtOverviewMovie.text.isNotEmpty() ||
                binding.txtGenresMovie.text.isNotEmpty() ||
                binding.txtBudgetMovie.text.isNotEmpty() ||
                binding.txtRevenueMovie.text.isNotEmpty()

        binding.sectionDetails.mostrarSi(hasContent)
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
            val adapter = HallazgoAdapter { hallazgo, cartel -> abrirOtra(hallazgo, cartel) }.also { it.submitList(similarMovies.map { p -> Hallazgo.de(p) }) }
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
            val adapter = HallazgoAdapter { hallazgo, cartel -> abrirOtra(hallazgo, cartel) }.also { it.submitList(recommendedMovies.map { p -> Hallazgo.de(p) }) }
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
        Avisos.breve(binding.root, message)
    }


    companion object {
        /**
         * Con que id se abre esta pantalla.
         *
         * Estaba escrita a mano en nueve sitios: en las tarjetas, en las listas, en la
         * filmografia, en el buscador... y cambiarla obligaba a buscarla por todo el
         * proyecto. Las pantallas nuevas ya declaraban la suya asi.
         */
        const val EXTRA_MOVIE_ID = "MOVIE_ID"
    }
}
