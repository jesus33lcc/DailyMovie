package com.example.dailymovie.utils

/**
 * Hace algo con el binding solo si la vista sigue existiendo.
 *
 * En un fragmento, `binding` es en realidad `_binding!!`, y `_binding` se pone a null en
 * `onDestroyView`. La capa de datos es de callbacks y no sabe nada de ciclos de vida: cuando
 * Firestore o TMDB contestan, el usuario puede haberse ido de la pestaña o haber girado el
 * movil, y ese `!!` revienta.
 *
 * No es rebuscado: crear una lista y cambiar de pestaña mientras se guarda ya cerraba la app.
 * **Cualquier callback de datos que toque la pantalla tiene que pasar por aqui.**
 *
 * ```kotlin
 * viewModel.crearLista(nombre) { resultado ->
 *     _binding.siLaVistaSigueAhi { vista -> Avisos.breve(vista.root, "Creada") }
 * }
 * ```
 *
 * @param bloque lo que se hace con el binding. No se llama si la vista ya no esta.
 */
inline fun <B : Any> B?.siLaVistaSigueAhi(bloque: (B) -> Unit) {
    this?.let(bloque)
}
