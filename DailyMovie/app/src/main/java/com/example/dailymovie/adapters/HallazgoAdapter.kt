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
    private val alTocar: (Hallazgo) -> Unit
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
        }

        if (hallazgo.tipo == TipoDeHallazgo.PERSONA) {
            // De una persona no hay nota que enseñar, y el subtitulo ya es su oficio.
            holder.subtitulo.text = hallazgo.subtitulo
            holder.nota.visibility = View.GONE
            holder.imagen.cargarFotoDePersona(hallazgo.imagen)
        } else {
            // Lo que aun no ha salido se dice en la propia tarjeta: con el año a secas no se
            // distingue de lo que ya se puede ver, y ademas todavia no tiene nota.
            val porVenir = Fechas.estaPorVenir(hallazgo.subtitulo)
            holder.subtitulo.text = if (porVenir) {
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

        holder.itemView.setOnClickListener { alTocar(hallazgo) }
    }

    private companion object {
        val COMPARADOR = object : DiffUtil.ItemCallback<Hallazgo>() {
            // El id se repite entre tipos en TMDB, asi que hay que mirar tambien que es.
            override fun areItemsTheSame(vieja: Hallazgo, nueva: Hallazgo) =
                vieja.id == nueva.id && vieja.tipo == nueva.tipo

            override fun areContentsTheSame(vieja: Hallazgo, nueva: Hallazgo) = vieja == nueva
        }
    }
}
