package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarCartel

/**
 * Algo que el usuario puede marcar en el onboarding.
 *
 * Se usa el mismo tipo para generos, peliculas y personas porque en pantalla se comportan
 * igual: una ficha con nombre, a veces con imagen, que se marca al tocarla.
 */
data class Elegible(
    val id: Int,
    val nombre: String,
    /** Ruta de TMDB (poster o foto). Null en los generos, que no tienen imagen. */
    val imagen: String?
)

class ElegibleAdapter(
    private val elementos: List<Elegible>,
    private val estaElegido: (Int) -> Boolean,
    private val alTocar: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<ElegibleAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_elegible, parent, false)
        return Holder(vista)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val elemento = elementos[position]
        holder.nombre.text = elemento.nombre

        if (elemento.imagen == null) {
            // Los generos no tienen cartel, asi que la ficha se queda solo con el nombre.
            holder.marco.visibility = View.GONE
        } else {
            holder.marco.visibility = View.VISIBLE
            holder.imagen.cargarCartel(elemento.imagen)
        }

        pintarMarca(holder, estaElegido(elemento.id))

        holder.itemView.setOnClickListener {
            val ahora = !estaElegido(elemento.id)
            alTocar(elemento.id, ahora)
            pintarMarca(holder, ahora)
        }
    }

    /** Un borde menta y la marca en la esquina para que se vea de un vistazo que esta elegido. */
    private fun pintarMarca(holder: Holder, elegido: Boolean) {
        holder.marca.visibility = if (elegido) View.VISIBLE else View.GONE
        holder.itemView.alpha = if (elegido) 1f else 0.75f
        holder.itemView.setBackgroundResource(
            if (elegido) R.drawable.rounded_border_box_big else R.drawable.rounded_border_box
        )
    }

    override fun getItemCount() = elementos.size

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val nombre: TextView = vista.findViewById(R.id.elegibleNombre)
        val imagen: ImageView = vista.findViewById(R.id.elegibleImagen)
        val marca: ImageView = vista.findViewById(R.id.elegibleMarca)
        val marco: View = vista.findViewById(R.id.elegibleMarco)
    }
}
