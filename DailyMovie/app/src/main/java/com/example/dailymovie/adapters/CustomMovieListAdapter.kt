package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.models.MovieModel
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.cargarCartel

/**
 * Las peliculas de una lista, en cuadricula.
 *
 * Antes eran filas de una en una con el cartel pequeño a la izquierda, asi que en una tablet
 * se veian cuatro peliculas y sobraba muchisimo ancho. En cuadricula entran bastantes mas de
 * un vistazo y manda el cartel, que es por lo que uno reconoce una pelicula.
 *
 * Cada tarjeta lleva sus tres puntos: el borrado deslizando no se descubria y ademas en una
 * cuadricula no se entiende, porque no esta claro que se esta arrastrando.
 */
class CustomMovieListAdapter(
    private val onMovieClick: (MovieModel) -> Unit,
    private val onOpciones: (MovieModel, View, Int) -> Unit
) : ListAdapter<MovieModel, CustomMovieListAdapter.MovieViewHolder>(COMPARADOR) {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txt_titleMovie)
        val year: TextView = itemView.findViewById(R.id.txt_yearMovie)
        val poster: ImageView = itemView.findViewById(R.id.img_posterMovie)
        val rating: TextView = itemView.findViewById(R.id.txt_voteAverageMovie)
        val opciones: ImageButton = itemView.findViewById(R.id.btnOpciones)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.title.text = movie.title
        holder.year.text = Fechas.soloElAno(movie.releaseDate)
        // Una estrella y la nota con una decimal: "Rating: 7.966" no lo dice nadie.
        holder.rating.text = holder.itemView.context.getString(R.string.nota_estrella, movie.voteAverage)
        holder.poster.cargarCartel(movie.posterPath)

        holder.itemView.setOnClickListener { onMovieClick(movie) }
        holder.opciones.setOnClickListener { boton ->
            onOpciones(movie, boton, holder.bindingAdapterPosition)
        }
    }

    /** La que esta en esa posicion, para cuando hay que devolverla a su sitio. */
    fun peliculaEn(position: Int): MovieModel = getItem(position)

    private companion object {
        val COMPARADOR = object : DiffUtil.ItemCallback<MovieModel>() {
            // Es la misma pelicula si comparten id, aunque le haya cambiado la nota.
            override fun areItemsTheSame(vieja: MovieModel, nueva: MovieModel) =
                vieja.id == nueva.id

            override fun areContentsTheSame(vieja: MovieModel, nueva: MovieModel) =
                vieja == nueva
        }
    }
}
