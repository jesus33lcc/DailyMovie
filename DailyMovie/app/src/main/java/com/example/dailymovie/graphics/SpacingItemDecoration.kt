package com.example.dailymovie.graphics

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.dailymovie.R

class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.top = spacing
        outRect.bottom = spacing
    }

    companion object {
        /**
         * El separado que usan todas las listas de la app.
         *
         * Antes cada pantalla se instanciaba el separador con un 8 escrito a mano, y el
         * @dimen que habia para esto no lo usaba nadie. Ademas ese 8 eran pixeles, no dp,
         * asi que el hueco se veia mas pequeño cuanto mas fina fuera la pantalla.
         */
        fun deLista(context: Context) = SpacingItemDecoration(
            context.resources.getDimensionPixelSize(R.dimen.recycler_view_spacing)
        )
    }
}
