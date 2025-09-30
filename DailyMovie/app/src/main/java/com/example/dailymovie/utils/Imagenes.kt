package com.example.dailymovie.utils

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.dailymovie.R

/**
 * Todas las imagenes de la app se cargan por aqui.
 *
 * Antes cada adapter y cada pantalla montaba su propio Glide con las mismas cuatro lineas
 * copiadas, y todas usaban el mismo icono gris de Android como relleno: daba igual que
 * faltara el cartel de una pelicula, la foto de un actor o la captura de un episodio.
 *
 * Aqui se decide una sola vez que se enseña mientras carga, que se enseña si no hay imagen y
 * como aparece. Si algun dia se cambia de libreria, se cambia en este fichero y ya.
 */
object Imagenes {

    /**
     * La ruleta que se ve mientras la imagen viene de internet.
     *
     * Hace falta una por carga: CircularProgressDrawable guarda el estado de la animacion,
     * asi que compartir una sola entre varias vistas hace que se paren unas a otras.
     */
    private fun ruleta(context: Context) = CircularProgressDrawable(context).apply {
        strokeWidth = 6f
        centerRadius = 24f
        setColorSchemeColors(ContextCompat.getColor(context, R.color.colorPrimary))
        start()
    }

    fun cargar(vista: ImageView, url: String?, @DrawableRes relleno: Int) {
        Glide.with(vista.context)
            .load(url)
            .placeholder(ruleta(vista.context))
            .error(relleno)
            // Sin esto la imagen aparece de golpe; con el fundido la pantalla se llena mucho
            // mas suave segun van llegando.
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .into(vista)
    }
}

/** El cartel de una pelicula o de una serie. */
fun ImageView.cargarCartel(ruta: String?) =
    Imagenes.cargar(this, ruta?.let { Constantes.IMAGE_URL + it }, R.drawable.relleno_pelicula)

/** La foto de un actor o de un director. */
fun ImageView.cargarFotoDePersona(ruta: String?) =
    Imagenes.cargar(this, ruta?.let { Constantes.IMAGE_URL + it }, R.drawable.relleno_persona)

/** Un fotograma: imagenes de la ficha, capturas de episodio, logos de plataforma. */
fun ImageView.cargarFotograma(ruta: String?) =
    Imagenes.cargar(this, ruta?.let { Constantes.IMAGE_URL + it }, R.drawable.relleno_fotograma)

/** Igual que el anterior pero con la direccion entera, para lo que no viene de TMDB. */
fun ImageView.cargarFotogramaDeUrl(url: String) =
    Imagenes.cargar(this, url, R.drawable.relleno_fotograma)

/** La version grande de la galeria a pantalla completa. */
fun ImageView.cargarImagenGrande(ruta: String?) =
    Imagenes.cargar(this, ruta?.let { Constantes.IMAGE_ORIGINAL_URL + it }, R.drawable.relleno_fotograma)
