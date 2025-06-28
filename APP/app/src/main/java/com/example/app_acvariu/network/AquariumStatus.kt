package com.example.app_acvariu.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AquariumApi {
    @GET("{command}")
    suspend fun getAquariumStatus(@Path("command") command: String): Response<String>

    // Add this function to send commands (like feeding time updates)
    @GET("{command}")
    suspend fun sendCommand(@Path("command") command: String): Response<String>

}





