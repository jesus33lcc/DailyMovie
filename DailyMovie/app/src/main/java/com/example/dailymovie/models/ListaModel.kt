package com.example.dailymovie.models

/**
 * Una lista de la pantalla de Listas.
 *
 * @property nombre el que se enseña y, en las listas del usuario, tambien el id del
 *   documento en Firestore: por eso no puede haber dos con el mismo.
 * @property icono el dibujo de la izquierda.
 * @property cuantas cuantas cosas tiene guardadas, o null si todavia no se sabe. Se cuenta
 *   despues de pintar la lista, porque hay que pedirla entera y no merece la pena hacer
 *   esperar al usuario solo por el numero.
 */
data class ListaModel(
    val nombre: String,
    val icono: Int,
    val cuantas: Int? = null
)
