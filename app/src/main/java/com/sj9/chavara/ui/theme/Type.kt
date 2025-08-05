package com.sj9.chavara.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import androidx.compose.ui.text.font.Font

val ris = FontFamily(
    Font(R.font.ris, FontWeight.Normal)
)

// Using system font as fallback for Inknut Antiqua since it's not available
val inknutAntiqua = FontFamily.Serif

val Typography = Typography(
    bodyLarge = TextStyle(
        // 3. Access the font directly by its variable name
        fontFamily = ris,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    // ... other text styles
)
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
