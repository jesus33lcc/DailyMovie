package com.example.dailymovie.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.utils.cargarFotoDePersona
import com.example.dailymovie.activities.views.PersonaA
import com.example.dailymovie.models.CastMemberModel
import com.example.dailymovie.utils.Constantes

/**
 * El reparto de la ficha, en una tira horizontal: foto, nombre y personaje.
 *
 * @param castList la gente que sale en pantalla. Se pinta en el orden en que llega, que es el
 *   de importancia del papel que ya manda TMDB.
 */
class CreditAdapter(private val castList: List<CastMemberModel>) : RecyclerView.Adapter<CreditAdapter.CreditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_credit, parent, false)
        return CreditViewHolder(view)
    }

    override fun onBindViewHolder(holder: CreditViewHolder, position: Int) {
        val castMember = castList[position]
        holder.imgCastProfile.cargarFotoDePersona(castMember.profilePath)
        holder.txtCastName.text = castMember.name
        holder.txtCharacterName.text = castMember.character

        // Tocar a alguien del reparto abre su ficha, con su biografia y su filmografia.
        holder.itemView.setOnClickListener {
            val contexto = holder.itemView.context
            contexto.startActivity(
                Intent(contexto, PersonaA::class.java)
                    .putExtra(PersonaA.EXTRA_PERSONA_ID, castMember.id)
            )
        }
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
