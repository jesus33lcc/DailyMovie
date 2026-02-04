package com.example.dailymovie.utils

import android.content.Context
import com.example.dailymovie.R
import com.example.dailymovie.data.MotivoDeRecomendacion
import com.example.dailymovie.data.TipoDeMotivo

/**
 * El texto de "por que te enseñamos esta pelicula".
 *
 * Vive aqui y no junto al recomendador para que la capa de datos no toque recursos de Android,
 * igual que pasa con [ErrorCarga.mensaje]. El recomendador dice **que** motivo es; el texto lo
 * elige la pantalla.
 *
 * @param context el de la pantalla que lo pinta.
 * @return la frase ya montada, con el nombre dentro si el motivo lo lleva.
 */
fun MotivoDeRecomendacion.comoTexto(context: Context): String = when (tipo) {
    TipoDeMotivo.GENTE_QUE_SIGUES ->
        context.getString(R.string.motivo_gente_que_sigues, nombre.orEmpty())
    TipoDeMotivo.GENTE_SIN_NOMBRE -> context.getString(R.string.motivo_gente_sin_nombre)
    TipoDeMotivo.COMO_TU_FAVORITA ->
        context.getString(R.string.motivo_como_tu_favorita, nombre.orEmpty())
    TipoDeMotivo.TUS_GENEROS -> context.getString(R.string.motivo_tus_generos)
    TipoDeMotivo.LO_POPULAR -> context.getString(R.string.motivo_lo_popular)
}
