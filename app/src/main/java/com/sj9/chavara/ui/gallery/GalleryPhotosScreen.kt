package com.sj9.chavara.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
fun GalleryPhotosScreen(
    photos: List<PhotoItem> = getDefaultPhotos(),
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
                .fillMaxSize()
                .alpha(0.8f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensions.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing)
        ) {
            // Top spacing
            Spacer(modifier = Modifier.height(dimensions.screenHeight * 0.06f))
            
            // Cy Photos title - responsive
            Text(
                text = "Cy Photos",
                color = Color.White,
                fontSize = responsiveFontSize(41f),
                fontWeight = FontWeight.W400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // January subtitle - responsive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "January",
                    color = Color.White,
                    fontSize = responsiveFontSize(29f),
                    fontWeight = FontWeight.W400,
                    modifier = Modifier
                            .offset(x = 3.dp, y = 4.dp)
                            .alpha(0.75f),
                )
            }

            // Photos grid - responsive adaptive grid


            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .offset(x = 3.dp, y = 0.dp)
                    .size(width = 362.dp, height = 597.dp)
                    .alpha(0.75f),
                contentPadding = PaddingValues(vertical = dimensions.verticalSpacing),
                horizontalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing * 2),
                verticalArrangement = Arrangement.spacedBy(dimensions.verticalSpacing * 1.5f)
            ) {
                items(photos) { photo ->
                    PhotoGridItem(photo = photo)
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: PhotoItem,
    modifier: Modifier = Modifier
) {

    val cardGradient = Brush.verticalGradient(
        0.0f to Color(0xFFCDD00E),
        1.0f to Color(0xFFD5BC1A)
    )
    val cornerRadius = getResponsiveCornerRadius()

    Box(
        modifier = modifier
            .size(width = 266.dp, height = 183.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(cardGradient)
            .alpha(0.85f),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for photo content
        // In a real app, you would load actual images here
        Text(
            text = photo.name,
            color = Color.White,
            fontSize = responsiveFontSize(14f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(getResponsiveSpacing())
        )
    }
}

data class PhotoItem(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)

private fun getDefaultPhotos(): List<PhotoItem> {
    // In a real app, this would fetch from a database or API
    // The number of items can grow dynamically based on actual content
    return (1..6).map { index ->
        PhotoItem(
            id = "photo_jan_$index",
            name = "Jan ${index}th",
            imageUrl = null // Would contain actual image URLs
        )
    }
}

@Preview(
    name = "Gallery Photos Screen - Medium",
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun GalleryPhotosScreenMediumPreview() {
    ChavaraTheme {
        GalleryPhotosScreen()
    }
}



