package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.activities.views.GaleriaA
import com.example.dailymovie.utils.Constantes

/** La tira de imagenes de la ficha. Al tocar una se abre la galeria a pantalla completa. */
class ImagenAdapter(private val rutas: List<String>) :
    RecyclerView.Adapter<ImagenAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_imagen, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(Constantes.IMAGE_URL + rutas[position])
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.imagen)

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
