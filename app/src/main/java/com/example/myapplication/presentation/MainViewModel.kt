package com.example.myapplication.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.WeatherRepositoryImpl
import com.example.myapplication.domain.GetWeatherByLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = WeatherRepositoryImpl()

    private val getWeatherByLocationUseCase = GetWeatherByLocationUseCase(repository)

    private val weatherApiState: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)

    val weatherApiStateLivedata = weatherApiState.asLiveData()
    fun getWeatherByLocation(location: String) {
        viewModelScope.launch {
            weatherApiState.value = ApiState.Loading
            getWeatherByLocationUseCase(location)
                .catch { e ->
                    weatherApiState.value = ApiState.Failure(e)
                }.collect { data ->
                    weatherApiState.value = ApiState.Success(data)
                }
        }

    }
}