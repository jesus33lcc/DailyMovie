package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotograma
import com.example.dailymovie.models.ProviderDetailModel

/**
 * Los logos de donde se puede ver algo, en la ficha.
 *
 * @param providerList las plataformas del pais del aparato, que ya vienen filtradas desde la
 *   pantalla: solo las de suscripcion, no las de alquiler ni compra.
 */
class ProviderAdapter(private val providerList: List<ProviderDetailModel>) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_provider, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providerList[position]
        holder.imgProviderLogo.cargarFotograma(provider.logoPath)
        holder.txtProviderName.text = provider.providerName
    }

    override fun getItemCount(): Int {
        return providerList.size
    }

    class ProviderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProviderLogo: ImageView = itemView.findViewById(R.id.imgProviderLogo)
        val txtProviderName: TextView = itemView.findViewById(R.id.txtProviderName)
    }
}