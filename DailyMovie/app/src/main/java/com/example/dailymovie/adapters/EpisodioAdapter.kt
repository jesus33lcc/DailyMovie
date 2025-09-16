package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
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

        holder.datos.text = datosDelEpisodio(episodio)

        Glide.with(holder.itemView.context)
            .load(Constantes.IMAGE_URL + episodio.imagen)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.imagen)
    }

    /** Fecha de emision y duracion, lo que quepa de los dos. */
    private fun datosDelEpisodio(episodio: EpisodioModel): String {
        val trozos = mutableListOf<String>()
        episodio.emision?.takeIf { it.isNotBlank() }?.let { trozos += Fechas.enLargo(it) }
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
