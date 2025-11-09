package com.example.dailymovie.client

import com.example.dailymovie.utils.Constantes
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * El unico sitio donde se monta Retrofit.
 *
 * Es un object para que la conexion se cree una sola vez y se comparta: cada Retrofit levanta
 * su propio OkHttp por debajo, con su pool de conexiones y sus hilos, y tener uno por pantalla
 * seria tirar memoria a la basura.
 *
 * Solo los repositorios deberian tocar esto. Una vista o un ViewModel que llame aqui se salta
 * la interfaz del repositorio y deja de poder probarse sin internet.
 */
object RetrofitClient {

    /**
     * La implementacion de [WebService] que genera Retrofit.
     *
     * Perezosa a proposito: asi no se monta nada al arrancar la app, solo la primera vez que
     * alguien pide datos de verdad.
     */
    val webService: WebService by lazy {
        Retrofit.Builder()
            .baseUrl(Constantes.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(WebService::class.java)
    }
}