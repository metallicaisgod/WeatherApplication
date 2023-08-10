package com.example.myapplication.data

import com.example.myapplication.domain.WeatherJson
import com.example.myapplication.domain.WeatherRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.io.Writer

class WeatherRepositoryImpl: WeatherRepository {

    override suspend fun getWeatherByLocation(location: String): Flow<WeatherJson> {
        return flow {
            val r = RetrofitInstance.getService().getWeatherByLocation(location)
            emit(r)
        }.flowOn(Dispatchers.IO)
    }
}