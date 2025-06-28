package com.example.app_acvariu.models

data class AquariumStatus(
    val temperature: String,  // e.g., "25.3 C"
    val waterLevel: String,   // e.g., "~50%"
    val time: String,          // e.g., "18:30"
    val light: String,    // e.g. "ON" or "OFF"
    val foodEmpty: Boolean,   // ← nou
    val fillFull: Boolean,
    val drainEmpty: Boolean,
    val tds: String,               // ex. "650ppm"
    val alertTdsHigh: Boolean,     // true dacă am primit ALERT:TDS_HIGH
    val motor: String   // <— add this
)
