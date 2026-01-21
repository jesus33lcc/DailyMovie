package com.example.dailymovie.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.dailymovie.R

/**
 * Los dialogos de la app, todos con la misma cara.
 *
 * Antes cada pantalla se montaba su AlertDialog, asi que salian con el blanco y el gris de
 * Material y los botones en mayusculas: al lado del azul marino y la Courier del resto
 * parecian de otra aplicacion. Aqui se construyen una sola vez y todos heredan la identidad.
 *
 * El bloque de aceptar recibe el dialogo para poder decidir si se cierra: hay casos, como
 * crear una lista con un nombre que no vale, en los que interesa dejarlo abierto y avisar
 * ahi mismo en vez de cerrarlo y soltar un Toast.
 */
object DialogoDailyMovie {

    /**
     * Monta el dialogo, lo enseña y lo devuelve.
     *
     * @param context el de la pantalla que lo abre.
     * @param titulo la linea de arriba, la unica que no se puede quitar.
     * @param mensaje la explicacion. Si no se pasa, ese hueco desaparece.
     * @param contenido una vista propia que se mete debajo del mensaje, para los dialogos que
     *   piden algo (el campo del nombre de una lista, la rejilla de casillas...).
     * @param textoAceptar el texto del boton de la derecha.
     * @param textoCancelar null para que no haya boton de cancelar, en los avisos que solo se
     *   leen y se cierran.
     * @param peligroso pinta el boton de aceptar en rojo. Para lo que no tiene vuelta atras:
     *   borrar una lista, borrar la cuenta.
     * @param alCancelar se llama al pulsar Cancelar. Ojo: tocar fuera tambien cierra el
     *   dialogo, y en ese caso no se llama a nadie.
     * @param alAceptar recibe el dialogo para poder decidir si se cierra o no. Si lo que se ha
     *   escrito no vale, interesa dejarlo abierto y avisar ahi mismo.
     * @return el dialogo ya en pantalla, por si hay que tocarlo despues.
     */
    fun mostrar(
        context: Context,
        titulo: String,
        mensaje: String? = null,
        contenido: View? = null,
        textoAceptar: String = context.getString(R.string.boton_aceptar_comun),
        textoCancelar: String? = context.getString(R.string.boton_cancelar_comun),
        peligroso: Boolean = false,
        alCancelar: (() -> Unit)? = null,
        alAceptar: (AlertDialog) -> Unit
    ): AlertDialog {
        val vista = LayoutInflater.from(context).inflate(R.layout.dialogo_dailymovie, null)

        vista.findViewById<TextView>(R.id.dialogoTitulo).text = titulo

        val textoMensaje = vista.findViewById<TextView>(R.id.dialogoMensaje)
        if (mensaje.isNullOrBlank()) {
            textoMensaje.visibility = View.GONE
        } else {
            textoMensaje.text = mensaje
        }

        contenido?.let { vista.findViewById<FrameLayout>(R.id.dialogoContenido).addView(it) }

        val dialogo = AlertDialog.Builder(context).setView(vista).create()
        // Sin esto el AlertDialog pone su propio fondo blanco debajo y se comen las esquinas
        // redondeadas del nuestro.
        dialogo.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val botonAceptar = vista.findViewById<TextView>(R.id.dialogoAceptar)
        botonAceptar.text = textoAceptar
        if (peligroso) botonAceptar.setBackgroundResource(R.drawable.dialogo_boton_peligro)
        botonAceptar.setOnClickListener { alAceptar(dialogo) }

        val botonCancelar = vista.findViewById<TextView>(R.id.dialogoCancelar)
        if (textoCancelar == null) {
            botonCancelar.visibility = View.GONE
        } else {
            botonCancelar.text = textoCancelar
            botonCancelar.setOnClickListener {
                dialogo.dismiss()
                alCancelar?.invoke()
            }
        }

        dialogo.show()
        return dialogo
    }

    /**
     * Atajo para las confirmaciones de toda la vida, que son casi todas.
     *
     * La diferencia con [mostrar] es que aqui el dialogo se cierra solo al aceptar, que es lo
     * que se quiere cuando no hay nada que validar.
     *
     * @param context el de la pantalla que lo abre.
     * @param titulo la pregunta, corta.
     * @param mensaje lo que pasa si dice que si.
     * @param textoAceptar el texto del boton, mejor el verbo ("Borrar") que un "Aceptar" seco.
     * @param peligroso pinta el boton de aceptar en rojo.
     * @param alCancelar se llama al pulsar Cancelar, no al tocar fuera.
     * @param alConfirmar lo que se hace si dice que si, con el dialogo ya cerrado.
     */
    fun confirmar(
        context: Context,
        titulo: String,
        mensaje: String,
        textoAceptar: String = context.getString(R.string.boton_aceptar_comun),
        peligroso: Boolean = false,
        alCancelar: (() -> Unit)? = null,
        alConfirmar: () -> Unit
    ) {
        mostrar(
            context = context,
            titulo = titulo,
            mensaje = mensaje,
            textoAceptar = textoAceptar,
            peligroso = peligroso,
            alCancelar = alCancelar
        ) { dialogo ->
            dialogo.dismiss()
            alConfirmar()
        }
    }
}
