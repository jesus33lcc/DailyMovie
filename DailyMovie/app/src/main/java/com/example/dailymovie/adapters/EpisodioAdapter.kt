package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotograma
import com.example.dailymovie.models.EpisodioModel
import com.example.dailymovie.utils.Constantes
import com.example.dailymovie.utils.Fechas

/** Los episodios de la temporada que este abierta. */
class EpisodioAdapter(private val episodios: List<EpisodioModel>) :
    RecyclerView.Adapter<EpisodioAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_episodio, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val episodio = episodios[position]

        holder.numero.text = episodio.numero.toString()
        holder.titulo.text = episodio.titulo
        holder.sinopsis.text = episodio.sinopsis.orEmpty()
        // Muchos episodios que aun no se han emitido no tienen sinopsis todavia.
        holder.sinopsis.visibility =
            if (episodio.sinopsis.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.datos.text = datosDelEpisodio(holder.itemView, episodio)

        holder.imagen.cargarFotograma(episodio.imagen)
    }

    /**
     * Fecha de emision y duracion, lo que quepa de los dos.
     *
     * Si el episodio todavia no se ha emitido se dice, porque TMDB los devuelve igual de
     * completos que los ya emitidos y solo con la fecha no se distingue: en una serie en
     * emision parecia que quedaban episodios por ver que en realidad aun no existen.
     */
    private fun datosDelEpisodio(vista: View, episodio: EpisodioModel): String {
        val trozos = mutableListOf<String>()
        val emision = episodio.emision?.takeIf { it.isNotBlank() }

        if (emision != null) {
            trozos += if (Fechas.estaPorVenir(emision)) {
                vista.context.getString(R.string.proximamente, Fechas.enLargo(emision))
            } else {
                Fechas.enLargo(emision)
            }
        }
        episodio.duracion?.takeIf { it > 0 }?.let { trozos += "$it min" }
        return trozos.joinToString(" · ")
    }

    override fun getItemCount() = episodios.size

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val numero: TextView = vista.findViewById(R.id.txtNumeroEpisodio)
        val titulo: TextView = vista.findViewById(R.id.txtTituloEpisodio)
        val sinopsis: TextView = vista.findViewById(R.id.txtSinopsisEpisodio)
        val datos: TextView = vista.findViewById(R.id.txtDatosEpisodio)
        val imagen: ImageView = vista.findViewById(R.id.imgEpisodio)
    }
}
