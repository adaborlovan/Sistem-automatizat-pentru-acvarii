package com.example.app_acvariu.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.app_acvariu.R

@Composable
fun FishAnimation(modifier: Modifier = Modifier) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.swimming_fish)
    )
    val composition = compositionResult.value

    val progress = animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    ).value

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier // <- Apply the passed‐in size modifier here
        )
    }
}
