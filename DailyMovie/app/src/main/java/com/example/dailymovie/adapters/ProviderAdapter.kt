package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.models.ProviderDetailModel
import com.example.dailymovie.utils.Constantes

class ProviderAdapter(private val providerList: List<ProviderDetailModel>) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_provider, parent, false)
        return ProviderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        val provider = providerList[position]
        Glide.with(holder.itemView.context)
            .load(Constantes.IMAGE_URL + provider.logoPath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.imgProviderLogo)
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