package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.models.Hallazgo
import com.example.dailymovie.models.TipoDeHallazgo
import com.example.dailymovie.utils.Fechas
import com.example.dailymovie.utils.cargarCartel
import com.example.dailymovie.utils.cargarFotoDePersona

/**
 * Los resultados de una busqueda, en cuadricula y mezclados.
 *
 * Peliculas, series y gente comparten tarjeta porque se enseña lo mismo de las tres. Lo que
 * cambia es la etiqueta de la esquina y, en las personas, que debajo va el oficio en vez del
 * año y no se enseña nota, que TMDB no da nota de la gente.
 */
class HallazgoAdapter(
    /**
     * Que hacer al tocar una tarjeta.
     *
     * Ademas del hallazgo se pasa su cartel, que hace falta para la animacion de abrir la
     * ficha: el cartel de la tarjeta y el de la ficha son "el mismo" y viaja de uno a otro.
     */
    private val alTocar: (Hallazgo, View) -> Unit
) : ListAdapter<Hallazgo, HallazgoAdapter.Holder>(COMPARADOR) {

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val imagen: ImageView = vista.findViewById(R.id.imgHallazgo)
        val titulo: TextView = vista.findViewById(R.id.txtTituloHallazgo)
        val subtitulo: TextView = vista.findViewById(R.id.txtSubtituloHallazgo)
        val nota: TextView = vista.findViewById(R.id.txtNotaHallazgo)
        val tipo: TextView = vista.findViewById(R.id.txtTipoHallazgo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_hallazgo, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val hallazgo = getItem(position)
        val contexto = holder.itemView.context

        holder.titulo.text = hallazgo.titulo
        holder.tipo.text = when (hallazgo.tipo) {
            TipoDeHallazgo.PELICULA -> "PELÍCULA"
            TipoDeHallazgo.SERIE -> "SERIE"
            TipoDeHallazgo.PERSONA -> "GENTE"
            TipoDeHallazgo.SAGA -> "SAGA"
        }

        if (hallazgo.tipo == TipoDeHallazgo.SAGA) {
            // Una saga no tiene año ni nota propios: los tiene cada pelicula de dentro.
            holder.subtitulo.text = "Todas las películas"
            holder.subtitulo.setTextColor(ContextCompat.getColor(contexto, R.color.colorPrimary))
            holder.nota.visibility = View.GONE
            holder.imagen.cargarCartel(hallazgo.imagen)
        } else if (hallazgo.tipo == TipoDeHallazgo.PERSONA) {
            // De una persona no hay nota que enseñar, y el subtitulo ya es su oficio.
            holder.subtitulo.text = hallazgo.subtitulo
            holder.nota.visibility = View.GONE
            holder.imagen.cargarFotoDePersona(hallazgo.imagen)
        } else {
            // Lo que aun no ha salido se dice en la propia tarjeta: con el año a secas no se
            // distingue de lo que ya se puede ver, y ademas todavia no tiene nota.
            val porVenir = Fechas.estaPorVenir(hallazgo.subtitulo)
            holder.subtitulo.text = if (hallazgo.subtituloLibre != null) {
                hallazgo.subtituloLibre
            } else if (porVenir) {
                contexto.getString(R.string.proximamente_corto)
            } else {
                Fechas.soloElAno(hallazgo.subtitulo)
            }
            holder.subtitulo.setTextColor(
                ContextCompat.getColor(
                    contexto,
                    if (porVenir) R.color.colorPeligroBorde else R.color.colorPrimary
                )
            )
            holder.nota.visibility =
                if (hallazgo.nota > 0 && !porVenir) View.VISIBLE else View.GONE
            holder.nota.text = contexto.getString(R.string.nota_estrella, hallazgo.nota)
            holder.imagen.cargarCartel(hallazgo.imagen)
        }

        // El nombre lleva el tipo ademas del id porque TMDB repite ids entre peliculas y
        // series, y dos vistas con el mismo nombre rompen la animacion.
        holder.imagen.transitionName = nombreDeTransicion(hallazgo)
        holder.itemView.setOnClickListener { alTocar(hallazgo, holder.imagen) }
    }

    companion object {
        /**
         * El nombre que une el cartel de la tarjeta con el de la ficha que se abre.
         *
         * Es publico porque la pantalla de destino tiene que ponerle exactamente el mismo a
         * su imagen: si no coinciden, no hay animacion y el cartel aparece de la nada.
         *
         * @param hallazgo el de la tarjeta que se ha tocado.
         * @return un nombre unico en la pantalla, con el tipo delante del id.
         */
        fun nombreDeTransicion(hallazgo: Hallazgo) = "cartel_${hallazgo.tipo}_${hallazgo.id}"

        private val COMPARADOR = object : DiffUtil.ItemCallback<Hallazgo>() {
            // El id se repite entre tipos en TMDB, asi que hay que mirar tambien que es.
            override fun areItemsTheSame(vieja: Hallazgo, nueva: Hallazgo) =
                vieja.id == nueva.id && vieja.tipo == nueva.tipo

            override fun areContentsTheSame(vieja: Hallazgo, nueva: Hallazgo) = vieja == nueva
        }
    }
}
