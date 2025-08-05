package com.sj9.chavara.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.ui.theme.ChavaraTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center

@Composable
fun AppInformationScreen(
    modifier: Modifier = Modifier
) {
    // Background gradient matching the design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 200f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .alpha(0.8f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB899123D4), // rgba(153, 35, 212, 0.72)
                                Color(0xA8872EB3), // rgba(135, 46, 179, 0.66)
                                Color(0xB961307A), // rgba(97, 48, 122, 0.72)
                                Color(0xB94F126E)  // rgba(79, 18, 110, 0.72)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Created by text with different font sizes
                val createdByText = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 24.sp)) {
                        append("Created By")
                    }
                    append(" \n")
                    withStyle(style = SpanStyle(fontSize = 30.sp)) {
                        append("Chavara Youth Members")
                    }
                }

                Text(
                    text = createdByText,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .alpha(0.8f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xB899123D4), // rgba(153, 35, 212, 0.72)
                                Color(0xA8872EB3), // rgba(135, 46, 179, 0.66)
                                Color(0xB961307A), // rgba(97, 48, 122, 0.72)
                                Color(0xB94F126E)  // rgba(79, 18, 110, 0.72)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Just a Beginning.......",
                    color = Color.Black,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(
    name = "App Information Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun AppInformationScreenPreview() {
    ChavaraTheme {
        AppInformationScreen()
    }
}
