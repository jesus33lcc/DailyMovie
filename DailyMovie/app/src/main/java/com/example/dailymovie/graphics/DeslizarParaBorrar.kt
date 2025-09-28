package com.example.dailymovie.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R

/**
 * Deslizar una fila a un lado para borrarla.
 *
 * Antes tambien se podia deslizar, pero el hueco que quedaba detras era transparente: no se
 * veia nada, asi que nadie descubria que se podia hacer y encima daba la sensacion de que la
 * fila se estaba rompiendo. Aqui aparece un fondo rojo con una papelera que va entrando
 * segun arrastras, para que quede claro que va a pasar antes de soltar.
 *
 * Quien decide que hacer al soltar es la pantalla, en alSoltar.
 */
class DeslizarParaBorrar(
    context: Context,
    private val alSoltar: (posicion: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val fondo = ColorDrawable(ContextCompat.getColor(context, R.color.colorPeligro))
    private val papelera = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
    private val margen = context.resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        alSoltar(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val fila = viewHolder.itemView

        // El fondo solo ocupa lo que la fila ya ha dejado a la vista, asi que parece que
        // asoma por debajo en vez de encenderse de golpe.
        when {
            dX > 0 -> fondo.setBounds(fila.left, fila.top, fila.left + dX.toInt(), fila.bottom)
            dX < 0 -> fondo.setBounds(fila.right + dX.toInt(), fila.top, fila.right, fila.bottom)
            else -> fondo.setBounds(0, 0, 0, 0)
        }
        fondo.draw(canvas)

        papelera?.let { icono ->
            val alto = (fila.bottom - fila.top - icono.intrinsicHeight) / 2
            val arriba = fila.top + alto
            val abajo = arriba + icono.intrinsicHeight

            // Solo se pinta cuando ya hay hueco suficiente, o se veria un icono aplastado
            // pegado al borde nada mas empezar a arrastrar.
            if (dX > icono.intrinsicWidth + margen) {
                icono.setBounds(
                    fila.left + margen,
                    arriba,
                    fila.left + margen + icono.intrinsicWidth,
                    abajo
                )
                icono.draw(canvas)
            } else if (dX < -(icono.intrinsicWidth + margen)) {
                icono.setBounds(
                    fila.right - margen - icono.intrinsicWidth,
                    arriba,
                    fila.right - margen,
                    abajo
                )
                icono.draw(canvas)
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
