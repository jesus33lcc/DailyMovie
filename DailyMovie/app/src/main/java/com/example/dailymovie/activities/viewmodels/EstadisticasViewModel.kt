package com.example.dailymovie.activities.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.UserRepository
import com.example.dailymovie.models.MovieModel

/**
 * Un dato de la pantalla de números.
 *
 * @property numero lo que se enseña en grande. Es texto y no un Int porque hay datos que no
 *   son una cuenta: la década más vista es "los 2000" y el género favorito, "Drama".
 * @property queEs la explicación de debajo, en minúscula y sin punto.
 */
data class Numero(val numero: String, val queEs: String)

/**
 * Los números del usuario: cuánto ha visto, de qué y de cuándo.
 *
 * Todo sale de lo que ya está guardado en Firestore, sin pedir nada a TMDB. Eso limita lo que
 * se puede contar (no hay duración de las películas, por ejemplo, así que no se puede decir
 * cuántas horas lleva) pero hace que la pantalla abra al momento y funcione sin cobertura,
 * porque Firestore tiene su copia offline.
 */
class EstadisticasViewModel(
    private val usuario: UserRepository = Dependencias.usuario
) : ViewModel() {

    private val _numeros = MutableLiveData<List<Numero>>()
    val numeros: LiveData<List<Numero>> get() = _numeros

    /**
     * Junta todo y publica la lista de una vez.
     *
     * Son cinco lecturas independientes de Firestore y se anidan a propósito: publicar según
     * llega cada una haría que la pantalla fuera dando saltos mientras se rellena.
     */
    fun cargar() {
        usuario.vistas { vistas ->
            usuario.favoritas { favoritas ->
                usuario.seriesVistas { seriesVistas ->
                    usuario.seriesEmpezadas { empezadas ->
                        usuario.listasDelUsuario { listas ->
                            _numeros.value = construir(
                                vistas, favoritas, seriesVistas.size, empezadas, listas.size
                            )
                        }
                    }
                }
            }
        }
    }

    private fun construir(
        vistas: List<MovieModel>,
        favoritas: List<MovieModel>,
        seriesVistas: Int,
        empezadas: Map<Int, Int>,
        listas: Int
    ): List<Numero> = buildList {
        add(Numero("${vistas.size}", enPlural(vistas.size, "película vista", "películas vistas")))
        add(Numero("${favoritas.size}", enPlural(favoritas.size, "favorita", "favoritas")))

        if (seriesVistas > 0) {
            add(Numero("$seriesVistas", enPlural(seriesVistas, "serie terminada", "series terminadas")))
        }
        if (empezadas.isNotEmpty()) {
            add(Numero("${empezadas.size}", enPlural(empezadas.size, "serie empezada", "series empezadas")))
            val episodios = empezadas.values.sum()
            add(Numero("$episodios", enPlural(episodios, "episodio visto", "episodios vistos")))
        }
        if (listas > 0) {
            add(Numero("$listas", enPlural(listas, "lista tuya", "listas tuyas")))
        }

        decadaMasVista(vistas + favoritas)?.let {
            add(Numero(it, "es la década que más ves"))
        }
        laMasAntigua(vistas + favoritas)?.let {
            add(Numero(it.title, "es lo más antiguo que has guardado"))
        }
    }

    /**
     * De qué década es la mayoría de lo que ha guardado.
     *
     * Se cuenta sobre vistas y favoritas juntas porque por separado casi nadie tiene bastantes
     * para que el dato signifique algo.
     */
    private fun decadaMasVista(peliculas: List<MovieModel>): String? {
        val decadas = peliculas
            .mapNotNull { it.releaseDate.take(4).toIntOrNull() }
            .map { it / 10 * 10 }
        if (decadas.isEmpty()) return null
        val ganadora = decadas.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return null
        // "los 90" se lee mejor que "los 1990", pero de 2000 en adelante hay que decir el año
        // entero o no se entiende.
        return if (ganadora in 1900..1990) "los ${ganadora % 100}" else "los $ganadora"
    }

    private fun laMasAntigua(peliculas: List<MovieModel>) = peliculas
        .filter { it.releaseDate.take(4).toIntOrNull() != null }
        .minByOrNull { it.releaseDate.take(4).toInt() }

    private fun enPlural(cuantos: Int, singular: String, plural: String) =
        if (cuantos == 1) singular else plural
}
