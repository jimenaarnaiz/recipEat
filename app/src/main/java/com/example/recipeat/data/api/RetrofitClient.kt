package com.example.recipeat.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.spoonacular.com/"

    // Creamos un cliente Retrofit con la base URL y el convertidor Gson para parsear la respuesta JSON
    val api: SpoonacularApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // La URL base de la API
            .addConverterFactory(GsonConverterFactory.create()) // Usamos Gson para parsear las respuestas JSON
            .build()
            .create(SpoonacularApi::class.java) // Creamos la instancia de la interfaz de la API
    }
}