package com.example.myapplication.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    @Volatile
    private var INSTANCE: Retrofit? = null

    private const val BASE_URL = "https://api.weatherapi.com/v1/"
    private const val API_KEY = "your_api_key_here"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            val originalHttpUrl = chain.request().url()
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", API_KEY)
                .addQueryParameter("days", "10")
                .addQueryParameter("aqi", "no")
                .addQueryParameter("alerts", "no")
                .build()
            request.url(url)
            chain.proceed(request.build())
        }.build()

    fun getService(): WeatherService{

        if(INSTANCE == null) {
            INSTANCE = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return  INSTANCE!!.create(WeatherService::class.java)
    }
}