package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R

/**
 * Las listas del usuario con una casilla cada una, para el dialogo de guardar una pelicula.
 *
 * Antes ese dialogo era un setItems: elegias una lista, se cerraba y a empezar de nuevo si
 * querias guardarla en dos sitios. Ademas no habia forma de ver donde la tenias ya metida ni
 * de sacarla desde ahi.
 *
 * El adapter no guarda nada por su cuenta: trabaja sobre el conjunto que le pasa el dialogo,
 * que es el que al final sabe que ha cambiado respecto a como estaba.
 *
 * @param nombres las listas del usuario, en el orden en que se enseñan.
 * @param marcadas las listas donde ya esta esa pelicula. Se modifica aqui segun se van
 *   tocando las casillas, y por eso quien lo pasa debe quedarse antes con una copia de como
 *   estaba: sin ella no hay forma de saber que hay que meter y que hay que sacar.
 */
class ListaCasillaAdapter(
    private val nombres: List<String>,
    private val marcadas: MutableSet<String>
) : RecyclerView.Adapter<ListaCasillaAdapter.CasillaViewHolder>() {

    class CasillaViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val casilla: CheckBox = vista.findViewById(R.id.casillaLista)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CasillaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lista_casilla, parent, false)
        return CasillaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CasillaViewHolder, position: Int) {
        val nombre = nombres[position]
        holder.casilla.text = nombre

        // El oyente se quita antes de poner el valor: al reciclar la fila, marcarla a mano
        // dispararia el oyente de la fila anterior y se apuntaria un cambio que nadie hizo.
        holder.casilla.setOnCheckedChangeListener(null)
        holder.casilla.isChecked = nombre in marcadas
        holder.casilla.setOnCheckedChangeListener { _, marcada ->
            if (marcada) marcadas.add(nombre) else marcadas.remove(nombre)
        }
    }

    override fun getItemCount() = nombres.size
}
