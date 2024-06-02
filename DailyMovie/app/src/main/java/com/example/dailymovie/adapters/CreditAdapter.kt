package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.dailymovie.R
import com.example.dailymovie.models.CastMemberModel
import com.example.dailymovie.utils.Constantes

class CreditAdapter(private val castList: List<CastMemberModel>) : RecyclerView.Adapter<CreditAdapter.CreditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_credit, parent, false)
        return CreditViewHolder(view)
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        val castMember = castList[position]
        Glide.with(holder.itemView.context)
            .load(Constantes.IMAGE_URL + castMember.profilePath)
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_image_24)
            .into(holder.imgCastProfile)
        holder.txtCastName.text = castMember.name
        holder.txtCharacterName.text = castMember.character
    }

    override fun getItemCount(): Int {
        return castList.size
    }

    class CreditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCastProfile: ImageView = itemView.findViewById(R.id.imgCastProfile)
        val txtCastName: TextView = itemView.findViewById(R.id.txtCastName)
        val txtCharacterName: TextView = itemView.findViewById(R.id.txtCharacterName)
    }
}
