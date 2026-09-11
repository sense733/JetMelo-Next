package com.rcmiku.music.ui.navigation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import com.rcmiku.music.constants.EmphasizedDecelerateEasing
import com.rcmiku.music.constants.NAV_TRANSITION_DURATION

@OptIn(ExperimentalSharedTransitionApi::class)
val JetMeloBoundsTransform = BoundsTransform { _, _ ->
    tween(
        durationMillis = NAV_TRANSITION_DURATION,
        easing = EmphasizedDecelerateEasing
    )
}
