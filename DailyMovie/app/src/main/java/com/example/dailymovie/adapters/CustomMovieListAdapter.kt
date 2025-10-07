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
import com.example.dailymovie.models.Guardado
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
    private val onMovieClick: (Guardado) -> Unit,
    private val onOpciones: (Guardado, View, Int) -> Unit
) : ListAdapter<Guardado, CustomMovieListAdapter.MovieViewHolder>(COMPARADOR) {

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
        holder.title.text = movie.titulo
        holder.year.text = Fechas.soloElAno(movie.fecha)
        // Una estrella y la nota con una decimal: "Rating: 7.966" no lo dice nadie.
        holder.rating.text = holder.itemView.context.getString(R.string.nota_estrella, movie.nota)
        holder.poster.cargarCartel(movie.cartel)

        holder.itemView.setOnClickListener { onMovieClick(movie) }
        holder.opciones.setOnClickListener { boton ->
            onOpciones(movie, boton, holder.bindingAdapterPosition)
        }
    }

    /** Lo que esta en esa posicion, para cuando hay que devolverlo a su sitio. */
    fun elementoEn(position: Int): Guardado = getItem(position)

    private companion object {
        val COMPARADOR = object : DiffUtil.ItemCallback<Guardado>() {
            // Una pelicula y una serie pueden compartir id en TMDB, asi que hay que mirar
            // tambien de que tipo es.
            override fun areItemsTheSame(vieja: Guardado, nueva: Guardado) =
                vieja.id == nueva.id && vieja.esSerie == nueva.esSerie

            override fun areContentsTheSame(vieja: Guardado, nueva: Guardado) =
                vieja == nueva
        }
    }
}
