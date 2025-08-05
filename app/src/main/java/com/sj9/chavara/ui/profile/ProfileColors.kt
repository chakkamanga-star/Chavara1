package com.sj9.chavara.ui.profile

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Color constants and gradients for the Profile screens
 * to match the design specifications exactly
 */
object ProfileColors {
    // Background gradient colors
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        )
    )

    // Form/content container gradient
    val contentGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xB899123D4), // rgba(153, 35, 212, 0.72)
            Color(0xA8872EB3), // rgba(135, 46, 179, 0.66)
            Color(0xB961307A), // rgba(97, 48, 122, 0.72)
            Color(0xB94F126E)  // rgba(79, 18, 110, 0.72)
        )
    )

    // Form field gradient
    val fieldGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xB8C69DDB), // rgba(198, 157, 219, 0.72)
            Color(0xA89F4CC9), // rgba(159, 76, 201, 0.66)
            Color(0xB87E38A1), // rgba(126, 56, 161, 0.72)
            Color(0xB86A238D)  // rgba(106, 35, 141, 0.72)
        )
    )

    // Individual colors
    val buttonBackground = Color(0x99AD79BF) // rgba(173, 121, 191, 0.60)
    val profileCircle = Color(0xFF642B5D) // #642B5D
    val changePhotoButton = Color(0xFF592F6B) // #592F6B
    val textColor = Color(0xFF0A0A0A) // #0A0A0A
}
