package com.example.dailymovie.graphics

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R
import com.example.dailymovie.adapters.HallazgoAdapter
import com.example.dailymovie.models.Hallazgo

/**
 * Pinta una tira horizontal de carteles.
 *
 * Este montaje —layout manager horizontal, la separacion si no la tiene ya, reutilizar el
 * adaptador si existe y `submitList`— estaba copiado en la portada, en series, en la ficha de
 * serie y en la de persona. Cuatro copias que se fueron separando: dos añadian la separacion
 * cada vez que llegaban datos, asi que el hueco entre carteles crecia solo.
 *
 * @param hallazgos lo que va en la tira.
 * @param seccion el bloque que envuelve la tira, si lo hay. Se esconde solo cuando no hay nada
 *   que enseñar, que es lo que se quiere en casi todas las secciones opcionales.
 * @param alTocar que hacer al tocar una tarjeta. Recibe tambien el cartel, para la animacion.
 */
fun RecyclerView.pintarTira(
    hallazgos: List<Hallazgo>,
    seccion: View? = null,
    alTocar: (Hallazgo, View) -> Unit
) {
    seccion?.visibility = if (hallazgos.isEmpty()) View.GONE else View.VISIBLE
    if (hallazgos.isEmpty() && seccion != null) return

    if (layoutManager == null) {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }
    // Solo la primera vez: añadirla en cada emision es lo que hacia crecer la separacion.
    if (itemDecorationCount == 0) {
        addItemDecoration(SpacingItemDecoration.deLista(context))
    }

    val adaptador = adapter as? HallazgoAdapter
        ?: HallazgoAdapter(alTocar).also { adapter = it }
    adaptador.submitList(hallazgos)
}

/**
 * Cuantos carteles caben a lo ancho.
 *
 * Se calcula en vez de fijar un numero porque la misma cuenta vale para un movil en vertical,
 * para el mismo movil girado y para una tablet, donde ademas el cartel es mas grande porque
 * sale de otro dimens. Estaba escrita en tres pantallas con tres variantes distintas de la
 * misma formula, asi que en tablet cada una acababa con un numero de columnas diferente.
 *
 * @return dos como minimo: con una sola columna esto dejaria de ser una cuadricula.
 */
fun Context.columnasDeCarteles(): Int {
    val anchoPantalla = resources.displayMetrics.widthPixels
    val anchoCartel = resources.getDimensionPixelSize(R.dimen.poster_ancho)
    val separacion = resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)
    return ((anchoPantalla - separacion * 4) / (anchoCartel + separacion * 2)).coerceAtLeast(2)
}
