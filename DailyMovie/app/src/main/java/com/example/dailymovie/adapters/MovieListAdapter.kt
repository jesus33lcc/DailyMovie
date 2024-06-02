package com.example.dailymovie.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.models.ListaModel

class MovieListAdapter(
    private val context: Context,
    private val listaList: MutableList<ListaModel>,
    private val itemClickListener: (ListaModel) -> Unit
) : RecyclerView.Adapter<MovieListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconoTypeListaImg: ImageView = view.findViewById(R.id.iconoTypeLista_img)
        val nombreListaTxt: TextView = view.findViewById(R.id.nombreLista_txt)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_lista_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lista = listaList[position]
        holder.nombreListaTxt.text = lista.nombre
        holder.iconoTypeListaImg.setImageResource(lista.icono)
        holder.itemView.setOnClickListener {
            itemClickListener(lista)
        }
    }

    override fun getItemCount(): Int {
        return listaList.size
    }

    fun removeItem(position: Int) {
        listaList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getList(): MutableList<ListaModel> {
        return listaList
    }
}
