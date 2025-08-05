package com.sj9.chavara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.ui.theme.*
import kotlin.math.min
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun WelcomeScreen(
    userName: String = "",
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        // Define scale factors based on your original design size
        val widthScale = maxWidth / 390.dp
        val heightScale = maxHeight / 844.dp
        val minScale = min(widthScale, heightScale)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Glassmorphic card
                Box(
                    modifier = Modifier
                        .width(330.dp * minScale)
                        .height(203.dp * minScale)
                        .clip(RoundedCornerShape(50.dp * minScale))
                        .border(
                            1.dp,
                            WhiteTransparent47,
                            RoundedCornerShape(50.dp * minScale)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to GlassmorphicPrimary,
                                    0.0f to GlassmorphicSecondary.copy(alpha = 0.65f),
                                    1.0f to GlassmorphicTertiary.copy(alpha = 0.30f)
                                ),
                                center = androidx.compose.ui.geometry.Offset(0.4442f, 0.2496f),
                                radius = 600f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Welcome title text
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 33.sp * minScale,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ris,
                                    color = Color.Black
                                )
                            ) {
                                append("Welcome $userName!")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 39.sp * minScale,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ris,
                                    color = Color.Black
                                )
                            ) {
                                append(" \n Chavara Youth!")
                            }
                        },
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp * minScale
                    )
                }

                Spacer(modifier = Modifier.height(30.dp * minScale))

                // "Let's gooo!!!" button
                Box(
                    modifier = Modifier
                        .width(221.dp * minScale)
                        .height(50.dp * minScale)
                        .clip(RoundedCornerShape(26.dp * minScale))
                        .border(
                            1.dp,
                            WhiteTransparent47,
                            RoundedCornerShape(26.dp * minScale)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to ButtonGlassPrimary,
                                    1.0f to ButtonGlassSecondary
                                ),
                                center = androidx.compose.ui.geometry.Offset(0.5f, 0.4375f),
                                radius = 150f
                            )
                        )
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(26.dp * minScale),
                            ambientColor = Color.Black.copy(alpha = 0.29f)
                        )
                        .clickable { onGetStarted() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Let's gooo!!!",
                        color = Color.Black,
                        fontSize = 31.sp * minScale,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ris,
                        lineHeight = 40.sp * minScale,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Welcome Screen",
    showBackground = true,
    widthDp = 384,
    heightDp = 917
)
@Composable
fun WelcomeScreenPreview() {
    ChavaraTheme {
        WelcomeScreen(
            onGetStarted = {}
        )
    }
}