// File: app/src/main/java/com/example/app_acvariu/utils/parseAquariumStatus.kt
package com.example.app_acvariu.utils

import com.example.app_acvariu.models.AquariumStatus

fun parseAquariumStatus(raw: String): AquariumStatus? {
    val cleanFull = raw.trim().removePrefix("!").removeSuffix("!")

    val foodEmpty = cleanFull.contains("ALERT:FOOD_EMPTY")

    val clean = cleanFull
        .replace(",ALERT:FOOD_EMPTY", "")
        .replace("ALERT:FOOD_EMPTY,", "")
        .replace("ALERT:FOOD_EMPTY", "")

    val parts = clean.split(",")
    if (parts.size < 4) return null

    val temperature = parts[0].substringAfter("T:")
    val waterLevel  = parts[1].substringAfter("W:")
    val time        = parts[2].substringAfter("R:")
    val light       = parts[3].substringAfter("L:")

    var fillFull   = cleanFull.contains("ALERT:FILL_FULL")
    val drainEmpty = cleanFull.contains("ALERT:DRAIN_EMPTY")

    val tds = parts.find { it.startsWith("D:") }?.substringAfter("D:") ?: "N/A"
    val alertTdsHigh = cleanFull.contains("ALERT:TDS_HIGH")

    val motor       = parts.find { it.startsWith("M:") }?.substringAfter("M:") ?: "OFF"

    return AquariumStatus(
        temperature = temperature,
        waterLevel  = waterLevel,
        time        = time,
        light       = light,
        foodEmpty   = foodEmpty,
        fillFull   = fillFull,
        drainEmpty = drainEmpty,
        tds            = tds,
        alertTdsHigh   = alertTdsHigh,
        motor         = motor
    )
}
