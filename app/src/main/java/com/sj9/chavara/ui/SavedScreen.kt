package com.sj9.chavara.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.utils.*

@Composable
fun SavedScreen(
    modifier: Modifier = Modifier
) {
    // Exact gradient from the design: linear-gradient(192deg, #80EF6F 14.35%, #1DA313 37.97%, #10810C 60.19%, #0E4808 79.64%)
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF80EF6F), // #80EF6F at 14.35%
            Color(0xFF1DA313), // #1DA313 at 37.97%
            Color(0xFF10810C), // #10810C at 60.19%
            Color(0xFF0E4808)  // #0E4808 at 79.64%
        ),
        start = androidx.compose.ui.geometry.Offset(350f, 0f),
        end = androidx.compose.ui.geometry.Offset(-50f, 800f)
    )

    // Button gradient: linear-gradient(180deg, #4EB154 27.88%, #30A038 61.06%, #338538 89.91%)
    val buttonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4EB154), // #4EB154 at 27.88%
            Color(0xFF30A038), // #30A038 at 61.06%
            Color(0xFF338538)  // #338538 at 89.91%
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, 400f)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        getResponsiveDimensions()
        
        // Background overlay image with opacity 0.2 (same positioning as in design)
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .width(388.dp)
                .height(690.dp)
                .offset(
                    x = 12.dp,
                    y = 139.dp
                )
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        // Main Jesus image (positioned as in design: 125px left, 278px top, 162x162px)
        Image(
            painter = painterResource(id = R.drawable.saved),
            contentDescription = "Jesus",
            modifier = Modifier
                .size(162.dp)
                .offset(
                    x = 125.dp,
                    y = 278.dp
                ),
            contentScale = ContentScale.Fit
        )

        // "Saved" button background (131x58px, positioned at 140px left, 473px top)
        Box(
            modifier = Modifier
                .width(131.dp)
                .height(58.dp)
                .offset(
                    x = 140.dp,
                    y = 473.dp
                )
                .clip(RoundedCornerShape(25.dp))
                .alpha(0.8f)
                .background(buttonGradient)
        )

        // "Saved" text (positioned at 161px left, 484px top)
        Text(
            text = "Saved",
            color = Color.White,
            fontFamily = ris,
            fontSize = responsiveFontSize(37f),
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .offset(
                    x = 151.dp,
                    y = 474.dp
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SavedScreenPreview() {
    ChavaraTheme {
        SavedScreen()
    }
}
