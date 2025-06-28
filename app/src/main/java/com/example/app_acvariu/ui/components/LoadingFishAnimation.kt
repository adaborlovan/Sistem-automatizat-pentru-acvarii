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
fun LoadingFishAnimation(modifier: Modifier = Modifier) {
    // Load the Lottie JSON from res/raw/loading_fish.json
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_fish)
    )
    val composition = compositionResult.value

    // Animate it indefinitely
    val progress = animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    ).value

    // Render the animation
    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier
        )
    }
}
