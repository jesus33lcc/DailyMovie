package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.models.ListaModel

/**
 * Los nombres de las listas: las dos fijas y las que ha creado el usuario.
 *
 * Igual que el de peliculas, va con submitList y comparador para que crear o borrar una
 * lista se vea entrar y salir en vez de repintarse la pantalla entera.
 *
 * @param itemClickListener al tocar la fila: abre esa lista.
 * @param onOpciones si es null, la fila no enseña los tres puntos. Es lo que distingue a
 *   Favoritos y Vistos, que vienen con la app y no se pueden borrar ni renombrar, de las
 *   listas que se ha hecho el usuario. Recibe la lista y el propio boton, que hace falta para
 *   colgar el menu de el.
 */
class MovieListAdapter(
    private val itemClickListener: (ListaModel) -> Unit,
    private val onOpciones: ((ListaModel, View) -> Unit)? = null
) : ListAdapter<ListaModel, MovieListAdapter.ViewHolder>(COMPARADOR) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconoTypeListaImg: ImageView = view.findViewById(R.id.iconoTypeLista_img)
        val nombreListaTxt: TextView = view.findViewById(R.id.nombreLista_txt)
        val cuantasTxt: TextView = view.findViewById(R.id.cuantasLista_txt)
        val opciones: ImageButton = view.findViewById(R.id.btnOpcionesLista)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lista_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lista = getItem(position)
        holder.nombreListaTxt.text = lista.nombre
        // Mientras no se sepa el numero no se enseña nada: un "0" mientras carga haria
        // pensar que la lista esta vacia cuando igual tiene veinte peliculas.
        holder.cuantasTxt.visibility = if (lista.cuantas == null) View.GONE else View.VISIBLE
        holder.cuantasTxt.text = lista.cuantas?.toString().orEmpty()
        holder.iconoTypeListaImg.setImageResource(lista.icono)
        holder.itemView.setOnClickListener {
            itemClickListener(lista)
        }

        if (onOpciones == null) {
            holder.opciones.visibility = View.GONE
        } else {
            holder.opciones.visibility = View.VISIBLE
            holder.opciones.setOnClickListener { boton -> onOpciones.invoke(lista, boton) }
        }
    }

    /**
     * La lista que esta en esa fila.
     *
     * @param position la fila, contando desde 0.
     * @return lo que hay pintado ahi ahora mismo.
     */
    fun listaEn(position: Int): ListaModel = getItem(position)

    private companion object {
        val COMPARADOR = object : DiffUtil.ItemCallback<ListaModel>() {
            // El nombre es el identificador: en Firestore es el id del documento.
            override fun areItemsTheSame(vieja: ListaModel, nueva: ListaModel) =
                vieja.nombre == nueva.nombre

            override fun areContentsTheSame(vieja: ListaModel, nueva: ListaModel) =
                vieja == nueva
        }
    }
}
