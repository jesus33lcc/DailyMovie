package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.models.TemporadaModel

/**
 * Los botones para elegir temporada.
 *
 * Se enseñan como "T1", "T2"... y se marca la que esta abierta. Los especiales (temporada
 * 0) se dejan fuera desde la pantalla, no aqui.
 *
 * Cual esta elegida se guarda aqui dentro, y arranca en la primera de la lista: al abrir una
 * serie siempre se ve algo, sin que la pantalla tenga que decir nada.
 *
 * @param temporadas las de la serie, en orden.
 * @param alElegir se llama al tocar una, y es donde la pantalla pide sus episodios.
 */
class TemporadaAdapter(
    private val temporadas: List<TemporadaModel>,
    private val alElegir: (TemporadaModel) -> Unit
) : RecyclerView.Adapter<TemporadaAdapter.Holder>() {

    private var elegida = temporadas.firstOrNull()?.numero ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_temporada, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val temporada = temporadas[position]
        holder.texto.text = "T${temporada.numero}"
        pintarSeleccion(holder, temporada.numero == elegida)

        holder.itemView.setOnClickListener {
            val anterior = temporadas.indexOfFirst { it.numero == elegida }
            elegida = temporada.numero
            if (anterior >= 0) notifyItemChanged(anterior)
            notifyItemChanged(position)
            alElegir(temporada)
        }
    }

    private fun pintarSeleccion(holder: Holder, estaElegida: Boolean) {
        holder.itemView.setBackgroundResource(
            if (estaElegida) R.drawable.rounded_border_box_big else R.drawable.rounded_border_box
        )
        holder.itemView.alpha = if (estaElegida) 1f else 0.7f
    }

    override fun getItemCount() = temporadas.size

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val texto: TextView = vista.findViewById(R.id.txtTemporada)
    }
}
