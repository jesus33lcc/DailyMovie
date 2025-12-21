package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Un botón de marcar y desmarcar que responde al instante y no se lía si lo aporreas.
 *
 * Los cuatro botones de guardar de la app (favorita y vista, en película y en serie) hacían lo
 * mismo y lo hacían mal: cada toque preguntaba a Firestore cómo estaba, escribía lo contrario y
 * después volvía a preguntar para pintar el icono. Tres viajes por toque, todos asíncronos.
 * Pulsando rápido, dos toques leían "no está" antes de que ninguno hubiera escrito, los dos la
 * metían, y el icono acababa enseñando lo que no era.
 *
 * Aquí manda la memoria:
 *
 * - **El icono cambia en el acto**, sin esperar a nadie. Lo que se ve es [puesto].
 * - **Nunca hay dos escrituras a la vez.** Mientras una está en marcha, los toques nuevos solo
 *   cambian lo que se quiere; al acabar se mira si hace falta otra pasada.
 * - **No se pierde el último toque.** Si cambiaste de idea mientras guardaba, se vuelve a
 *   guardar con lo último, así que se acaba en lo que pidió el usuario.
 * - **Si falla, se deshace.** El icono vuelve a donde estaba en vez de mentir.
 *
 * De propina, aporrear el botón cinco veces son como mucho dos escrituras, no cinco.
 *
 * @param guardar cómo se guarda de verdad. Recibe cómo tiene que quedar y avisa de si pudo.
 */
class Marcado(private val guardar: (Boolean, (Boolean) -> Unit) -> Unit) {

    private val _puesto = MutableLiveData(false)

    /** Cómo hay que pintar el botón ahora mismo. */
    val puesto: LiveData<Boolean> get() = _puesto

    /** Si hay una escritura en marcha. Mientras esté a true no se lanza otra. */
    private var guardando = false

    /** Lo último que Firestore confirmó. Es a donde se vuelve si algo falla. */
    private var confirmado: Boolean? = null

    /**
     * Cómo está al abrir la ficha.
     *
     * @param valor lo que dice Firestore. Se da por bueno sin escribir nada.
     */
    fun empezarEn(valor: Boolean) {
        _puesto.value = valor
        confirmado = valor
    }

    /**
     * Le da la vuelta.
     *
     * @param alFallar se llama si no se pudo guardar, después de deshacer el cambio en
     *   pantalla, por si la pantalla quiere avisar al usuario.
     */
    fun cambiar(alFallar: (() -> Unit)? = null) {
        _puesto.value = !(_puesto.value ?: false)
        guardarSiHaceFalta(alFallar)
    }

    /**
     * Escribe, pero solo si toca.
     *
     * Se llama a sí misma al terminar cada escritura: es la forma de recoger los toques que
     * hayan llegado mientras tanto sin encolar una escritura por cada uno.
     */
    private fun guardarSiHaceFalta(alFallar: (() -> Unit)?) {
        if (guardando) return
        val quiero = _puesto.value ?: return
        if (quiero == confirmado) return

        guardando = true
        guardar(quiero) { bien ->
            guardando = false
            if (!bien) {
                // Se vuelve a lo último que sí se guardó. Si nunca se guardó nada, a false,
                // que es como nace cualquier ficha.
                _puesto.value = confirmado ?: false
                alFallar?.invoke()
                return@guardar
            }
            confirmado = quiero
            guardarSiHaceFalta(alFallar)
        }
    }
}
