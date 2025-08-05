package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.utils.*

@Composable
fun GalleryMainScreen(
    onNavigateToPhotos: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.linearGradient(
        0.0f to Color(0xFFC38732),
        0.4028f to Color(0xFFDCB72F),
        0.588f to Color(0xFFD3CA15),
        0.7594f to Color(0xFFDDE05F),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(412f, 917f)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val dimensions = getResponsiveDimensions()
        
        // Background image with opacity - responsive
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 7.dp, y = 189.dp)
                .size(width = 359.dp, height = 515.dp)
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensions.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing * 2)
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.115f))
            
            // Gallery title - responsive
            Text(
                text = "Gallery",
                color = Color.White,
                fontSize = responsiveFontSize(52f),
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Navigation cards - responsive grid layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing * 1.5f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // First row - Photos and Members
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        dimensions.verticalSpacing,
                        Alignment.CenterHorizontally
                    )
                ) {
                    GalleryNavigationCard(
                        title = "Cy Photos",
                        onClick = onNavigateToPhotos,
                        modifier = Modifier.weight(1f)
                            .alpha(0.7f)
                    )
                    
                    GalleryNavigationCard(
                        title = "Members",
                        onClick = onNavigateToMembers,
                        modifier = Modifier.weight(1f)
                            .alpha(0.7f)
                    )
                }

                // Second row - Videos (centered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GalleryNavigationCard(
                        title = "Cy Videos",
                        onClick = onNavigateToVideos,
                        modifier = Modifier.fillMaxWidth(0.5f)
                            .alpha(0.7f)
                    )
                }
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(dimensions.verticalSpacing))
        }
    }
}

@Composable
private fun GalleryNavigationCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cornerRadius = getResponsiveCornerRadius()
    
    Box(
        modifier = modifier
            .aspectRatio(0.78f) // Maintain consistent aspect ratio
            .clickable { onClick() }
    ) {
        // Card background with gradient
        val cardGradient = Brush.verticalGradient(
            0.2547f to Color(0xFFFFF700),
            0.4279f to Color(0xFFDCD727),
            0.6106f to Color(0xFFB4B028),
            0.7885f to Color(0xFF949027)
        )

        // Custom path shape - responsive rounded rectangle
        Box(
            modifier = Modifier
                .fillMaxSize(0.87f) // Slightly smaller than container
                .align(Alignment.Center)
                .clip(
                    RoundedCornerShape(
                        topStart = cornerRadius * 2,
                        topEnd = cornerRadius,
                        bottomStart = cornerRadius,
                        bottomEnd = cornerRadius * 2
                    )
                )
                .background(cardGradient)
        ) {
            // Shine overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(
                        RoundedCornerShape(
                            topStart = cornerRadius * 2,
                            topEnd = cornerRadius,
                            bottomStart = cornerRadius,
                            bottomEnd = cornerRadius * 2
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Title text - responsive
        Text(
            text = title,
            color = Color.White,
            fontSize = responsiveFontSize(27f),
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = getResponsivePadding())
        )
    }
}

@Preview(
    name = "Gallery Main Screen - Medium",
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun GalleryMainScreenMediumPreview() {
    ChavaraTheme {
        GalleryMainScreen(
            onNavigateToPhotos = {},
            onNavigateToVideos = {},
            onNavigateToMembers = {}
        )
    }
}




