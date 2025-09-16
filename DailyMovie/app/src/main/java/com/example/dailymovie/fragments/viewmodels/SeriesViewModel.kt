package com.example.dailymovie.fragments.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dailymovie.data.Dependencias
import com.example.dailymovie.data.Resultado
import com.example.dailymovie.data.SerieRepository
import com.example.dailymovie.models.SerieModel
import com.example.dailymovie.utils.ErrorCarga

/** Las listas de series de la pestaña nueva. */
class SeriesViewModel(
    private val series: SerieRepository = Dependencias.series
) : ViewModel() {

    private val _populares = MutableLiveData<List<SerieModel>>()
    val populares: LiveData<List<SerieModel>> get() = _populares

    private val _enEmision = MutableLiveData<List<SerieModel>>()
    val enEmision: LiveData<List<SerieModel>> get() = _enEmision

    private val _mejorValoradas = MutableLiveData<List<SerieModel>>()
    val mejorValoradas: LiveData<List<SerieModel>> get() = _mejorValoradas

    private val _error = MutableLiveData<ErrorCarga?>()
    val error: LiveData<ErrorCarga?> get() = _error

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> get() = _cargando

    private var pendientes = 0

    fun cargar() {
        pendientes = TOTAL_PETICIONES
        _cargando.value = true
        series.populares { reparte(it) { lista -> _populares.value = lista } }
        series.enEmision { reparte(it) { lista -> _enEmision.value = lista } }
        series.mejorValoradas { reparte(it) { lista -> _mejorValoradas.value = lista } }
    }

    private fun reparte(resultado: Resultado<List<SerieModel>>, alIrBien: (List<SerieModel>) -> Unit) {
        when (resultado) {
            is Resultado.Exito -> alIrBien(resultado.datos)
            is Resultado.Fallo -> _error.value = resultado.motivo
        }
        pendientes--
        if (pendientes <= 0) {
            pendientes = 0
            _cargando.value = false
        }
    }

    fun errorMostrado() {
        _error.value = null
    }

    private companion object {
        const val TOTAL_PETICIONES = 3
    }
}
