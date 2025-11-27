package com.example.dailymovie.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.client.response.ResenaResponse
import com.example.dailymovie.utils.Fechas

/**
 * Las reseñas de una pelicula.
 *
 * Cada una se enseña recortada y se abre al tocarla. Que estado tiene abierta cada una se
 * guarda aqui por id y no en el ViewHolder: los ViewHolder se reciclan, y si se guardara en
 * ellos al bajar y volver a subir aparecerian abiertas reseñas que nadie habia tocado.
 *
 * @param resenas las que trae TMDB, ya sin las que vienen sin texto.
 */
class ResenaAdapter(
    private val resenas: List<ResenaResponse>
) : RecyclerView.Adapter<ResenaAdapter.Holder>() {

    private val abiertas = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_resena, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val resena = resenas[position]
        val contexto = holder.itemView.context

        holder.autor.text = resena.autor
        holder.texto.text = sinMarcas(resena.texto)

        val nota = resena.detallesDelAutor?.nota
        holder.nota.visibility = if (nota == null || nota <= 0) View.GONE else View.VISIBLE
        if (nota != null) holder.nota.text = contexto.getString(R.string.nota_estrella, nota)

        // La fecha viene como "2024-03-12T18:04:22.000Z"; sobra todo menos el dia.
        val dia = resena.fecha?.take(10).orEmpty()
        holder.fecha.text = Fechas.enLargo(dia)
        holder.fecha.visibility = if (dia.isBlank()) View.GONE else View.VISIBLE

        pintarSiEstaAbierta(holder, resena.id in abiertas)

        holder.itemView.setOnClickListener {
            val ahora = resena.id !in abiertas
            if (ahora) abiertas.add(resena.id) else abiertas.remove(resena.id)
            pintarSiEstaAbierta(holder, ahora)
        }
    }

    /**
     * Quita las marcas de Markdown del texto.
     *
     * TMDB deja escribir las reseñas en Markdown pero devuelve el texto en crudo, asi que
     * sin esto se leen cosas como "Great Movie **Ever**". Se quitan las marcas en vez de
     * pintarlas en negrita porque la Courier de la app no tiene cursiva y la negrita en
     * mitad de un parrafo largo distrae mas que ayuda.
     */
    private fun sinMarcas(texto: String): String = texto
        .replace(Regex("\\*\\*|__|\\*|_"), "")
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    private fun pintarSiEstaAbierta(holder: Holder, abierta: Boolean) {
        holder.texto.maxLines = if (abierta) Int.MAX_VALUE else LINEAS_RECORTADA
        holder.leer.text = if (abierta) "Cerrar" else "Leer entera"
        // El "Leer entera" solo tiene sentido si de verdad hay algo escondido. Hay que
        // esperar a que el TextView se mida: hasta entonces no sabe cuantas lineas ocupa.
        holder.texto.post {
            val recortada = holder.texto.layout?.let { it.lineCount >= LINEAS_RECORTADA } ?: false
            holder.leer.visibility = if (abierta || recortada) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount() = resenas.size

    class Holder(vista: View) : RecyclerView.ViewHolder(vista) {
        val autor: TextView = vista.findViewById(R.id.txtAutorResena)
        val nota: TextView = vista.findViewById(R.id.txtNotaResena)
        val fecha: TextView = vista.findViewById(R.id.txtFechaResena)
        val texto: TextView = vista.findViewById(R.id.txtTextoResena)
        val leer: TextView = vista.findViewById(R.id.txtLeerResena)
    }

    private companion object {
        const val LINEAS_RECORTADA = 6
    }
}
