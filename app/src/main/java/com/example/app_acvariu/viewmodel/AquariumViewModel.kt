package com.example.app_acvariu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_acvariu.models.AquariumStatus
import com.example.app_acvariu.network.RetrofitClient
import com.example.app_acvariu.utils.parseAquariumStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log


class AquariumViewModel : ViewModel() {
    private val _status = MutableStateFlow<AquariumStatus?>(null)
    val status: StateFlow<AquariumStatus?> = _status

    fun fetchAquariumStatus() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.aquariumApi.getAquariumStatus("!U:ALL!")

                if (response.isSuccessful) {
                    val responseBody = response.body().orEmpty()
                    Log.d("RAW_STATUS", responseBody)
                    val parsed = parseAquariumStatus(responseBody)
                    _status.value = parsed
                }
            } catch (e: Exception) {

            }
        }
    }

    fun updateLightState(on: Boolean) {
        _status.value = _status.value?.copy(light = if (on) "ON" else "OFF")
        viewModelScope.launch {
            try {
                val cmd = if (on) "!PL:ON!" else "!PL:OFF!"
                RetrofitClient.aquariumApi.sendCommand(cmd)
            } catch (e: Exception) {
                fetchAquariumStatus()
            }
        }
    }

    fun updateExtractionPumpState(on: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = if (on) "!PE:ON!" else "!PE:OFF!"
                RetrofitClient.aquariumApi.sendCommand(cmd)
            } catch (e: Exception) { /*…*/ }
        }
    }

    fun updateAddingPumpState(on: Boolean) {
        viewModelScope.launch {
            try {
                val cmd = if (on) "!PA:ON!" else "!PA:OFF!"
                RetrofitClient.aquariumApi.sendCommand(cmd)
            } catch (e: Exception) { /*…*/ }
        }
    }

    fun updateFishCount(count: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.aquariumApi.sendCommand("!Q:$count!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    /*   fun updatePortions(count: Int) {
        viewModelScope.launch {
            try {
                RetrofitClient.aquariumApi.sendCommand("!Q:$count!")
            } catch (e: Exception) { /*…*/ }
        }
    }
*/
    fun updateFeedingTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                val hh = hour.toString().padStart(2, '0')
                val mm = minute.toString().padStart(2, '0')
                val cmd = "!F:$hh:$mm!"
                RetrofitClient.aquariumApi.sendCommand(cmd)
            } catch (e: Exception) { /*…*/ }
        }
    }

    fun resetFoodCounter() {
        viewModelScope.launch {
            try {
                RetrofitClient.aquariumApi.sendCommand("!RESET_FOOD!")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetFillTimer() = viewModelScope.launch {
        RetrofitClient.aquariumApi.sendCommand("!RESET_FILL!")
    }
    fun resetDrainTimer() = viewModelScope.launch {
        RetrofitClient.aquariumApi.sendCommand("!RESET_DRAIN!")
    }

    /** Trimite comanda pentru modul manual **/
    fun setManualMode() = viewModelScope.launch {
        try {
            RetrofitClient.aquariumApi.sendCommand("!MODE:M!")
        } catch (e: Exception) {

        }
    }

    /** Trimite comanda pentru modul automat **/
    fun setAutoMode() = viewModelScope.launch {
        try {
            RetrofitClient.aquariumApi.sendCommand("!MODE:A!")
        } catch (e: Exception) {
            // eventual tratezi eroarea
        }
    }



    // in AquariumViewModel.kt
    fun updateMotorState(on: Boolean) {
        // optimistic UI
        _status.value = _status.value?.copy(motor = if (on) "ON" else "OFF")
        viewModelScope.launch {
            try {
                val cmd = if (on) "!MTR:ON!" else "!MTR:OFF!"
                RetrofitClient.aquariumApi.sendCommand(cmd)
            } catch(e: Exception) {
                fetchAquariumStatus()
            }
        }
    }


}
