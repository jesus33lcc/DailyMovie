package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotoDePersona
import com.example.dailymovie.utils.cargarFotograma
import com.example.dailymovie.activities.views.GaleriaA
import com.example.dailymovie.utils.Constantes

/**
 * La tira de imagenes de la ficha. Al tocar una se abre la galeria a pantalla completa.
 *
 * @param rutas las de TMDB, sin la direccion delante. Se le pasan enteras a la galeria junto
 *   con la posicion tocada, para poder pasar de una a otra sin volver atras.
 */
class ImagenAdapter(
    private val rutas: List<String>,
    /**
     * Si lo que se enseña son retratos de una persona y no fotogramas.
     *
     * Las fotos de TMDB de un actor son verticales; con la medida apaisada de los fotogramas
     * se les recortaba media cara. Solo cambia el tamaño de la miniatura: la galeria a
     * pantalla completa las enseña igual de bien en los dos casos.
     */
    private val sonRetratos: Boolean = false
) : RecyclerView.Adapter<ImagenAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_imagen, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (sonRetratos) {
            val recursos = holder.itemView.resources
            holder.imagen.layoutParams = holder.imagen.layoutParams.apply {
                width = recursos.getDimensionPixelSize(R.dimen.foto_persona_ancho)
                height = recursos.getDimensionPixelSize(R.dimen.foto_persona_alto)
            }
            holder.imagen.cargarFotoDePersona(rutas[position])
        } else {
            holder.imagen.cargarFotograma(rutas[position])
        }

        holder.itemView.setOnClickListener {
            val contexto = holder.itemView.context
            contexto.startActivity(
                Intent(contexto, GaleriaA::class.java)
                    .putStringArrayListExtra(GaleriaA.EXTRA_IMAGENES, ArrayList(rutas))
                    .putExtra(GaleriaA.EXTRA_POSICION, position)
            )
        }
    }

    override fun getItemCount() = rutas.size

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val imagen: ImageView = vista.findViewById(R.id.imagenMiniatura)
    }
}
