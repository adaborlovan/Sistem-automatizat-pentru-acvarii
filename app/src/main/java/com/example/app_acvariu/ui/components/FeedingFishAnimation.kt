package com.example.app_acvariu.ui.components

import androidx.compose.foundation.layout.Box
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
fun FeedingFishAnimation(modifier: Modifier = Modifier) {
    // 1. Load the composition from raw resource
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.feeding_fish)
    )
    val composition = compositionResult.value

    // 2. Create an animation state that will loop forever
    val animationState = animateLottieCompositionAsState(
        composition    = composition,
        iterations     = LottieConstants.IterateForever,
        isPlaying      = true,
        speed          = 1.0f
    )
    val progress = animationState.value

    // 3. Only render when we have a loaded composition
    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress    = { progress },
            modifier    = modifier
        )
    }
}
